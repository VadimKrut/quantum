/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javafx.concurrent.Task;
import javafx.scene.layout.FlowPane;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryIdentityService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSkeletonHydrogens;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.CdkCompoundIdentifiers;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.PubChemIdentityClient;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Offline caption editing with explicit, graph-verified IUPAC lookup in the same dialog. */
final class LaboratoryComponentNaming {
  private LaboratoryComponentNaming() {}

  /** Caller guards any other unfinished form before opening this dialog. */
  static boolean show(final LaboratoryEditor editor, final String componentId, final Window owner) {
    return create(editor, componentId, owner).showAndWait().orElse(false);
  }

  static Dialog<Boolean> create(
      final LaboratoryEditor editor, final String componentId, final Window owner) {
    return create(editor, componentId, owner, new PubChemIdentityClient());
  }

  static Dialog<Boolean> create(
      final LaboratoryEditor editor,
      final String componentId,
      final Window owner,
      final PubChemIdentityClient client) {
    Objects.requireNonNull(editor);
    Objects.requireNonNull(componentId);
    Objects.requireNonNull(client);
    final var expected = editor.document();
    final var component =
        expected.experiment().components().stream()
            .filter(item -> item.id().equals(componentId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Выберите вещество для переименования."));
    final var drawing =
        Objects.requireNonNull(
            expected.layouts().get(componentId), "Структура вещества не найдена.");
    final Dialog<Boolean> dialog = new Dialog<>();
    dialog.setTitle("Название вещества");
    if (owner != null) {
      dialog.initOwner(owner);
    }
    LaboratoryDialogStyle.apply(dialog, owner);
    dialog.getDialogPane().getStyleClass().add("laboratory-component-naming");
    final TextField name = new TextField(drawing.molecule().displayName());
    name.setId("chemistry-component-name");
    name.setPromptText("Своё название или обозначение");
    name.setMaxWidth(Double.MAX_VALUE);
    name.getStyleClass().add("laboratory-name-field");
    final Label heading = new Label("Как назвать вещество?");
    heading.getStyleClass().add("laboratory-name-heading");
    final Label context =
        new Label(drawing.molecule().formula().hillNotation() + " · название на схеме и в списках");
    context.getStyleClass().add("laboratory-name-context");
    final Label hint =
        new Label(
            "Введите своё имя без интернета или найдите IUPAC по структуре. Копировать ничего не нужно.");
    hint.getStyleClass().add("laboratory-name-hint");
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    final Label error = new Label();
    error.setId("chemistry-component-name-error");
    error.setWrapText(true);
    error.setMinHeight(Region.USE_PREF_SIZE);
    error.getStyleClass().add("laboratory-inline-message");
    error.managedProperty().bind(error.textProperty().isNotEmpty());
    error.visibleProperty().bind(error.managedProperty());
    final Button lookup = new Button("Найти IUPAC");
    lookup.setId("chemistry-component-name-lookup");
    lookup.getStyleClass().add("laboratory-secondary-action");
    lookup.setTooltip(
        new Tooltip("Найти название в PubChem: отправляется только SMILES этой структуры."));
    final Button stop = new Button("Остановить");
    stop.setId("chemistry-component-name-stop");
    stop.setVisible(false);
    stop.setManaged(false);
    final Button restore = new Button("Вернуть моё имя");
    restore.setId("chemistry-component-name-restore");
    restore.setVisible(false);
    restore.setManaged(false);
    final Label status = new Label("Поиск в PubChem требует интернета.");
    status.setId("chemistry-component-name-status");
    status.getStyleClass().add("laboratory-name-status");
    status.setWrapText(true);
    status.setMinHeight(Region.USE_PREF_SIZE);
    final FlowPane actions = new FlowPane(8, 8, lookup, stop, restore);
    actions.getStyleClass().add("laboratory-name-actions");
    final VBox content = new VBox(12, heading, context, name, hint, actions, status, error);
    content.setMinWidth(0);
    content.setPrefWidth(540);
    content.setMaxWidth(640);
    dialog.getDialogPane().setContent(content);
    final ButtonType apply = new ButtonType("Применить имя", ButtonBar.ButtonData.OK_DONE);
    final ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().setAll(apply, cancel);
    final Button applyButton = (Button) dialog.getDialogPane().lookupButton(apply);
    applyButton.setId("chemistry-component-name-apply");
    applyButton.getStyleClass().add("laboratory-primary-action");
    dialog.getDialogPane().lookupButton(cancel).setId("chemistry-component-name-cancel");
    final AtomicBoolean changed = new AtomicBoolean();
    final AtomicBoolean disposed = new AtomicBoolean();
    final AtomicReference<Task<MolecularIdentity>> running = new AtomicReference<>();
    final AtomicReference<MolecularIdentity> found = new AtomicReference<>();
    final AtomicReference<MolecularIdentity> previousFound = new AtomicReference<>();
    final AtomicReference<String> previousName = new AtomicReference<>();
    final java.util.function.Consumer<Boolean> busy =
        value -> {
          name.setDisable(value);
          lookup.setDisable(value);
          applyButton.setDisable(value);
          restore.setDisable(value);
          stop.setVisible(value);
          stop.setManaged(value);
        };
    final Runnable interrupt =
        () -> {
          final var task = running.getAndSet(null);
          if (task != null) {
            task.cancel(true);
          }
          busy.accept(false);
        };
    final Runnable close =
        () -> {
          disposed.set(true);
          interrupt.run();
        };
    dialog
        .getDialogPane()
        .lookupButton(cancel)
        .addEventFilter(ActionEvent.ACTION, event -> close.run());
    dialog.setOnCloseRequest(event -> close.run());
    dialog.setOnHidden(event -> close.run());
    stop.setOnAction(
        event -> {
          interrupt.run();
          status.setText("Поиск остановлен. Можно ввести своё имя или повторить поиск.");
        });
    restore.setOnAction(
        event -> {
          if (previousName.get() != null) {
            name.setText(previousName.get());
            found.set(previousFound.get());
          }
          restore.setVisible(false);
          restore.setManaged(false);
          status.setText(
              "Возвращено имя до поиска. Нажмите «Применить имя», чтобы сохранить его в опыте.");
        });
    lookup.setOnAction(
        event -> {
          if (disposed.get() || running.get() != null) {
            return;
          }
          if (!expected.equals(editor.document())) {
            error.setText(
                "Опыт изменился. Откройте переименование заново; введённое имя остаётся в поле.");
            return;
          }
          error.setText("");
          busy.accept(true);
          status.setText("Ищем IUPAC в PubChem и проверяем структуру…");
          final String entered = name.getText();
          final MolecularIdentity prior = found.get();
          final Task<MolecularIdentity> task =
              new Task<>() {
                @Override
                protected MolecularIdentity call() throws Exception {
                  if (MoleculeSkeletonHydrogens.managed(drawing.molecule())
                      && MoleculeSkeletonHydrogens.unsupportedCount(drawing) > 0) {
                    throw new IllegalArgumentException(
                        "Сначала проверьте H, заряд и валентность структуры. Своё имя можно ввести вручную.");
                  }
                  final var result = client.lookup(drawing, "", true);
                  final var previous =
                      MolecularIdentity.read(drawing.molecule())
                          .filter(
                              identity -> new CdkCompoundIdentifiers().current(drawing, identity))
                          .orElse(null);
                  return previous == null
                      ? result
                      : LaboratoryIdentityService.preserveEnteredNames(
                          drawing,
                          result,
                          previous.commonName(),
                          previous.synonyms(),
                          previous.source());
                }
              };
          running.set(task);
          task.setOnSucceeded(
              done -> {
                if (disposed.get() || running.get() != task) {
                  return;
                }
                running.set(null);
                busy.accept(false);
                if (!expected.equals(editor.document())) {
                  error.setText(
                      "Опыт изменился во время поиска. Результат не применён; введённое имя сохранено.");
                  status.setText("Откройте переименование заново.");
                  return;
                }
                final var result = task.getValue();
                previousName.set(entered);
                previousFound.set(prior);
                found.set(result);
                name.setText(result.iupacName());
                restore.setVisible(true);
                restore.setManaged(true);
                name.positionCaret(0);
                status.setText(
                    "Название найдено · PubChem CID "
                        + result.pubchemCid()
                        + ". Структура совпадает. Нажмите «Применить имя».");
              });
          task.setOnFailed(
              done -> {
                if (disposed.get() || running.get() != task) {
                  return;
                }
                running.set(null);
                busy.accept(false);
                status.setText(searchFailure(task.getException()));
              });
          final Thread worker = new Thread(task, "compound-name-lookup");
          worker.setDaemon(true);
          worker.start();
        });
    applyButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          try {
            if (disposed.get()) {
              event.consume();
              return;
            }
            if (running.get() != null) {
              throw new IllegalArgumentException("Дождитесь результата или остановите поиск.");
            }
            final String text = name.getText().strip();
            if (text.isEmpty()) {
              throw new IllegalArgumentException("Введите название вещества.");
            }
            if (!expected.equals(editor.document())) {
              throw new IllegalArgumentException(
                  "Опыт изменился. Введённое имя сохранено в поле; откройте переименование заново.");
            }
            if (!text.equals(drawing.molecule().displayName()) || found.get() != null) {
              final var source = drawing.molecule();
              // Do not use graph-edit helpers: renaming must not normalize H, stereo or metadata.
              final var renamed =
                  found.get() != null
                      ? MolecularIdentity.attach(source, text, found.get())
                      : Molecule.of(
                          source.id(),
                          text,
                          source.atoms(),
                          source.bonds(),
                          source.charge(),
                          source.spinMultiplicity(),
                          source.stereochemistry(),
                          source.conformation(),
                          source.opticalRotation(),
                          source.symmetry(),
                          source.metadata());
              editor.replace(
                  componentId,
                  MoleculeDrawing.of(
                      renamed, drawing.atoms(), drawing.bonds(), drawing.manualFields()),
                  component.role());
              changed.set(true);
            }
            close.run();
          } catch (final RuntimeException failure) {
            error.setText(
                Objects.requireNonNullElse(
                    failure.getMessage(), "Не удалось изменить имя. Ввод сохранён."));
            event.consume();
          }
        });
    dialog.setResultConverter(button -> button == apply && changed.get());
    dialog.setOnShown(
        event -> {
          name.requestFocus();
          name.selectAll();
        });
    return dialog;
  }

  private static String searchFailure(final Throwable failure) {
    Throwable cause = failure;
    while (cause != null
        && cause.getCause() != null
        && !(cause instanceof IllegalArgumentException)) {
      cause = cause.getCause();
    }
    if (cause instanceof java.net.http.HttpTimeoutException
        || cause instanceof java.net.SocketTimeoutException) {
      return "PubChem не ответил вовремя. Повторите поиск или введите своё имя.";
    }
    if (cause instanceof java.io.IOException
        || cause instanceof java.nio.channels.UnresolvedAddressException) {
      return "Не удалось связаться с PubChem. Проверьте интернет или введите своё имя — оно сохранится без сети.";
    }
    return Objects.requireNonNullElse(
        cause == null ? null : cause.getMessage(), "Поиск недоступен. Можно ввести своё имя.");
  }

  /** Keep the complete value; only its rendering contracts to the caller's available width. */
  static Label label(final String fullName) {
    final Label label = new Label(Objects.requireNonNullElse(fullName, ""));
    label.setWrapText(false);
    label.setTextOverrun(OverrunStyle.ELLIPSIS);
    label.setEllipsisString("…");
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    label.setTooltip(new Tooltip(label.getText()));
    return label;
  }

  /** Display-only formatting; selection, values and a caller's converter are untouched. */
  static <T> void choices(final ComboBox<T> box, final Function<T, String> text) {
    Objects.requireNonNull(box);
    Objects.requireNonNull(text);
    box.setButtonCell(choiceCell(box, text));
    box.setCellFactory(list -> choiceCell(box, text));
  }

  private static <T> ListCell<T> choiceCell(final ComboBox<T> box, final Function<T, String> text) {
    final ListCell<T> cell =
        new ListCell<>() {
          @Override
          protected void updateItem(final T item, final boolean empty) {
            super.updateItem(item, empty);
            this.setGraphic(null);
            final String name =
                empty || item == null ? null : Objects.requireNonNullElse(text.apply(item), "");
            this.setText(name);
            this.setTooltip(name == null || name.isBlank() ? null : new Tooltip(name));
          }
        };
    cell.setWrapText(false);
    cell.setTextOverrun(OverrunStyle.ELLIPSIS);
    cell.setEllipsisString("…");
    cell.setMinWidth(0);
    cell.prefWidthProperty().bind(Bindings.max(1, box.widthProperty().subtract(26)));
    cell.setMaxWidth(Region.USE_PREF_SIZE);
    return cell;
  }
}
