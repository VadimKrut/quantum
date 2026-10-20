/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.math.BigDecimal;
import java.util.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryLiterature.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryCrossrefClient;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.LaboratoryArticleLinks;

/** Bibliography, qualified source yields and explicit reuse as unexecuted procedure steps. */
final class LaboratoryLiteraturePage extends LaboratoryComputationPage {
  private record Choice(String id, String title) {
    @Override
    public String toString() {
      return title;
    }
  }

  private final LaboratoryEditor editor;
  private final Runnable save;
  private final java.util.function.Consumer<java.net.URI> linkOpener;
  private final LaboratoryArticleLinks articleLinks = new LaboratoryArticleLinks();
  private final TextArea mirrorAddresses = new TextArea();
  private final MenuButton sciHub = new MenuButton("Sci-Hub");
  private LaboratoryDrawing expected;
  private final ListView<Reference> references = new ListView<>();
  private final ListView<Method> methods = new ListView<>();
  private final TextField doi = new TextField(),
      title = new TextField(),
      authors = new TextField(),
      year = new TextField(),
      url = new TextField(),
      supplement = new TextField();
  private final TextArea referenceNotes = new TextArea();
  private final Label referenceCopy = note(""), methodCopy = note("");
  private final VBox attachments = new VBox(5);
  private final Map<String, CheckBox> attachmentChecks = new LinkedHashMap<>();
  private final TextField methodTitle = new TextField(),
      reportedYield = new TextField(),
      yieldBasis = new TextField();
  private final TextArea procedure = new TextArea(), methodNotes = new TextArea();
  private final ComboBox<Reference> methodReference = new ComboBox<>();
  private final ComboBox<Choice> stage = new ComboBox<>();
  private final TabPane tabs = new TabPane();
  private final Button fetch, toMethod, reuse;
  private final TitledPane referenceDetails = new TitledPane(), methodDetails = new TitledPane();
  private final Label referenceState = note(""), methodState = note("");
  private String referenceId, methodId;
  private boolean changing, referenceDirty, methodDirty;
  private Thread worker;

  LaboratoryLiteraturePage(
      final LaboratoryEditor editor,
      final String stageId,
      final Runnable save,
      final Runnable close) {
    this(editor, stageId, save, close, null);
  }

  LaboratoryLiteraturePage(
      final LaboratoryEditor editor,
      final String stageId,
      final Runnable save,
      final Runnable close,
      final java.util.function.Consumer<java.net.URI> linkOpener) {
    super("Литература и методики", close);
    this.linkOpener = linkOpener;
    this.editor = editor;
    this.expected = editor.document();
    this.save = save;
    this.setId("chemistry-literature-page");
    this.changing = true;
    this.doi.setId("chemistry-literature-doi");
    this.procedure.setId("chemistry-literature-procedure");
    this.doi.setPromptText("10.xxxx/… или https://doi.org/…");
    this.year.setPromptText("Год");
    this.reportedYield.setPromptText("Не указан");
    this.yieldBasis.setPromptText("Например: выделенный продукт, ЯМР с внутренним стандартом");
    this.reportedYield.setPrefColumnCount(10);
    this.yieldBasis.setPrefColumnCount(36);
    this.referenceNotes.setPrefRowCount(3);
    this.methodNotes.setPrefRowCount(3);
    this.procedure.setPrefRowCount(8);
    this.procedure.setMinHeight(160);
    this.procedure.setWrapText(true);
    this.referenceNotes.setWrapText(true);
    this.methodNotes.setWrapText(true);
    this.stage.getItems().add(new Choice(null, "Весь опыт · без привязки к стадии"));
    this.expected
        .experiment()
        .stages()
        .forEach(s -> this.stage.getItems().add(new Choice(s.id(), s.title())));
    this.stage.setValue(
        this.stage.getItems().stream()
            .filter(s -> Objects.equals(s.id(), stageId))
            .findFirst()
            .orElse(this.stage.getItems().getFirst()));
    this.methodReference.setMaxWidth(Double.MAX_VALUE);
    this.stage.setMaxWidth(Double.MAX_VALUE);
    for (final var attachment : this.expected.experiment().attachments()) {
      final var check = new CheckBox(attachment.name());
      check.setWrapText(true);
      check
          .selectedProperty()
          .addListener(
              (o, a, b) -> {
                if (!changing) {
                  referenceDirty = true;
                  updateState();
                }
              });
      this.attachmentChecks.put(attachment.id(), check);
      this.attachments.getChildren().add(check);
    }
    if (this.attachmentChecks.isEmpty()) {
      this.attachments
          .getChildren()
          .add(
              note(
                  "Локальный PDF или SI сначала добавьте во вложения опыта; затем свяжите его здесь."));
    }
    this.fetch = button("Заполнить по DOI", this::lookup);
    this.fetch.setId("literature-fetch-doi");
    this.url.setId("chemistry-literature-url");
    this.referenceDetails.setText("Ссылки и подробности источника");
    this.referenceDetails.setId("literature-reference-details");
    this.referenceDetails.setAnimated(false);
    this.referenceDetails.setExpanded(false);
    this.mirrorAddresses.setId("literature-mirror-addresses");
    this.mirrorAddresses.setText(String.join("\n", this.articleLinks.mirrors()));
    this.mirrorAddresses.setPrefRowCount(2);
    this.mirrorAddresses.setWrapText(false);
    this.sciHub.setId("literature-sci-hub");
    this.sciHub.setTooltip(
        new Tooltip("Открыть DOI на выбранном адресе; доступность не проверена"));
    this.refreshArticleLinks();
    final Button saveMirrors = button("Сохранить адреса", this::saveArticleLinks);
    saveMirrors.setId("literature-save-mirrors");
    this.referenceDetails.setContent(
        new VBox(
            10,
            field("Год", this.year),
            field("Статья · URL", this.url),
            field("Supporting information · URL", this.supplement),
            field("Локальные вложения опыта", this.attachments),
            field("Происхождение и примечания", this.referenceNotes),
            field("Адреса Sci-Hub · по одному в строке", this.mirrorAddresses),
            note("Адреса могут меняться; список можно отредактировать. Доступность не проверена."),
            saveMirrors));
    final Button saveSource = button("Сохранить источник", this::saveReference);
    saveSource.getStyleClass().add("laboratory-primary-action");
    saveSource.setId("literature-save-source");
    this.toMethod = button("Добавить методику", this::methodForReference);
    this.toMethod.setId("literature-next-method");
    final Button openArticle = button("Открыть статью", this::openArticle);
    openArticle.setId("literature-open-article");
    openArticle.setTooltip(new Tooltip("Открыть указанную ссылку в системном браузере"));
    final MenuButton sourceMore =
        more(
            menu("Новый источник / сбросить поля", this::newReference),
            menu("Открыть SI", () -> openLink(this.supplement.getText())),
            menu("Удалить источник", this::removeReference));
    final var articleForm =
        new VBox(
            10,
            note("Источник связывает статью, методику и литературный выход с этим опытом."),
            field("DOI", new HBox(8, this.doi, this.fetch)),
            note(
                "По DOI заполняются сведения о публикации. Полный текст открывается отдельно по ссылке на статью."),
            field("Название публикации *", this.title),
            field("Авторы", this.authors),
            new FlowPane(8, 8, saveSource, this.toMethod, openArticle, this.sciHub, sourceMore),
            this.referenceState,
            this.referenceCopy,
            this.referenceDetails);
    HBox.setHgrow(this.doi, Priority.ALWAYS);
    articleForm.setPadding(new Insets(12));
    this.methodDetails.setText("Замечания к адаптации");
    this.methodDetails.setId("literature-method-details");
    this.methodDetails.setAnimated(false);
    this.methodDetails.setExpanded(false);
    this.methodDetails.setContent(this.methodNotes);
    final Button saveMethod = button("Сохранить методику", this::saveMethod);
    saveMethod.getStyleClass().add("laboratory-primary-action");
    saveMethod.setId("literature-save-method");
    this.reuse = button("Добавить в план работы", this::reuseMethod);
    this.reuse.setId("literature-reuse-method");
    final var methodForm =
        new VBox(
            10,
            field("Источник *", this.methodReference),
            field("Название методики *", this.methodTitle),
            field("Стадия", this.stage),
            field("Процедура *", this.procedure),
            new FlowPane(
                10,
                8,
                field("Литературный выход, %", this.reportedYield),
                field("Основание указанного выхода", this.yieldBasis)),
            note("Литературный выход хранится отдельно от результатов вашего опыта."),
            new FlowPane(
                8,
                8,
                saveMethod,
                this.reuse,
                more(
                    menu("Новая методика / сбросить поля", this::newMethod),
                    menu("Удалить методику", this::removeMethod))),
            this.methodState,
            this.methodCopy,
            this.methodDetails);
    methodForm.setPadding(new Insets(12));
    methodForm.setMinHeight(Region.USE_PREF_SIZE);
    articleForm.setMinHeight(Region.USE_PREF_SIZE);
    final var refsSplit = new SplitPane(this.references, formScroll(articleForm));
    refsSplit.setDividerPositions(0.29);
    final var methodsSplit = new SplitPane(this.methods, formScroll(methodForm));
    methodsSplit.setDividerPositions(0.29);
    this.references.setMinWidth(0);
    this.methods.setMinWidth(0);
    this.references.setPrefHeight(130);
    this.methods.setPrefHeight(130);
    this.widthProperty()
        .addListener(
            o -> {
              final boolean narrow = getWidth() < 950;
              for (final var split : List.of(refsSplit, methodsSplit)) {
                final var orientation =
                    narrow
                        ? javafx.geometry.Orientation.VERTICAL
                        : javafx.geometry.Orientation.HORIZONTAL;
                if (split.getOrientation() != orientation) {
                  split.setOrientation(orientation);
                  split.setDividerPositions(narrow ? .22 : .29);
                }
              }
            });
    this.references.setPlaceholder(note("Добавьте первый источник"));
    this.methods.setPlaceholder(note("Сохраните источник, затем добавьте методику"));
    this.references.setCellFactory(
        v ->
            new ListCell<>() {
              {
                prefWidthProperty().bind(v.widthProperty().subtract(20));
                setMaxWidth(USE_PREF_SIZE);
              }

              @Override
              protected void updateItem(final Reference r, final boolean empty) {
                super.updateItem(r, empty);
                setText(null);
                setGraphic(empty || r == null ? null : cellNote(r.citation(), this));
              }
            });
    this.methods.setCellFactory(
        v ->
            new ListCell<>() {
              {
                prefWidthProperty().bind(v.widthProperty().subtract(20));
                setMaxWidth(USE_PREF_SIZE);
              }

              @Override
              protected void updateItem(final Method m, final boolean empty) {
                super.updateItem(m, empty);
                setText(null);
                setGraphic(
                    empty || m == null
                        ? null
                        : cellNote(
                            m.title()
                                + (m.literatureYieldPercent() == null
                                    ? ""
                                    : "\nЛитература: "
                                        + m.literatureYieldPercent().toPlainString()
                                        + "% · "
                                        + m.yieldBasis()),
                            this));
              }
            });
    this.references
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing && b != null) {
                if (referenceDirty) {
                  status.setText(
                      "Сохраните или сбросьте поля источника перед выбором другой карточки.");
                  changing = true;
                  references.getSelectionModel().select(a);
                  changing = false;
                } else {
                  loadReference(b);
                }
              }
            });
    this.methods
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing && b != null) {
                if (methodDirty) {
                  status.setText(
                      "Сохраните или сбросьте поля методики перед выбором другой карточки.");
                  changing = true;
                  methods.getSelectionModel().select(a);
                  changing = false;
                } else {
                  loadMethod(b);
                }
              }
            });
    final var referenceTab = new Tab("Источники", refsSplit);
    final var methodTab = new Tab("Методики и выходы", methodsSplit);
    referenceTab.setClosable(false);
    methodTab.setClosable(false);
    this.tabs.getTabs().addAll(referenceTab, methodTab);
    this.setCenter(tabs);
    this.status.setPadding(new Insets(10, 16, 12, 16));
    this.setBottom(status);
    for (final var input : List.of(doi, title, authors, year, url, supplement, referenceNotes)) {
      input
          .textProperty()
          .addListener(
              (o, a, b) -> {
                if (!changing) {
                  referenceDirty = true;
                  updateState();
                }
              });
    }
    for (final var input :
        List.of(methodTitle, reportedYield, yieldBasis, procedure, methodNotes)) {
      input
          .textProperty()
          .addListener(
              (o, a, b) -> {
                if (!changing) {
                  methodDirty = true;
                  updateState();
                }
              });
    }
    methodReference
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                methodDirty = true;
                updateState();
              }
            });
    stage
        .valueProperty()
        .addListener(
            (o, a, b) -> {
              if (!changing) {
                methodDirty = true;
                updateState();
              }
            });
    this.refresh();
    this.changing = false;
    this.updateState();
    this.status.setText(
        this.articleLinks.warning().isBlank()
            ? "Источники и методики сохраняются вместе с опытом. Повтор переносит ссылки и план, без фактических результатов."
            : this.articleLinks.warning());
  }

  private void refresh() {
    final boolean before = changing;
    changing = true;
    final String selected =
        methodReference.getValue() == null ? null : methodReference.getValue().id();
    references.getItems().setAll(editor.document().experiment().literature().references());
    methods.getItems().setAll(editor.document().experiment().literature().methods());
    methodReference.getItems().setAll(references.getItems());
    methodReference.setValue(
        methodReference.getItems().stream()
            .filter(r -> r.id().equals(selected))
            .findFirst()
            .orElse(
                methodReference.getItems().isEmpty()
                    ? null
                    : methodReference.getItems().getFirst()));
    changing = before;
  }

  private void loadReference(final Reference r) {
    changing = true;
    referenceId = r.id();
    doi.setText(r.doi());
    title.setText(r.title());
    authors.setText(r.authors());
    year.setText(r.year() == 0 ? "" : Integer.toString(r.year()));
    url.setText(r.url());
    supplement.setText(r.supplementUrl());
    referenceNotes.setText(r.notes());
    referenceCopy.setText(r.copyNote());
    attachmentChecks.forEach((id, check) -> check.setSelected(r.attachmentIds().contains(id)));
    changing = false;
    referenceDirty = false;
    updateState();
  }

  private void loadMethod(final Method m) {
    changing = true;
    methodId = m.id();
    methodTitle.setText(m.title());
    procedure.setText(m.procedureText());
    reportedYield.setText(
        m.literatureYieldPercent() == null ? "" : m.literatureYieldPercent().toPlainString());
    yieldBasis.setText(m.yieldBasis());
    methodNotes.setText(m.notes());
    methodCopy.setText(m.copyNote());
    methodReference.setValue(
        methodReference.getItems().stream()
            .filter(r -> r.id().equals(m.referenceId()))
            .findFirst()
            .orElse(null));
    stage.setValue(
        stage.getItems().stream()
            .filter(s -> Objects.equals(s.id(), m.stageId()))
            .findFirst()
            .orElse(stage.getItems().getFirst()));
    changing = false;
    methodDirty = false;
    updateState();
  }

  private void newReference() {
    changing = true;
    referenceId = null;
    referenceCopy.setText("");
    references.getSelectionModel().clearSelection();
    for (final var input : List.of(doi, title, authors, year, url, supplement, referenceNotes)) {
      input.clear();
    }
    attachmentChecks.values().forEach(c -> c.setSelected(false));
    changing = false;
    referenceDirty = false;
    if (!methodDirty) {
      expected = editor.document();
    }
    referenceDetails.setExpanded(false);
    updateState();
  }

  private void newMethod() {
    changing = true;
    methodId = null;
    methodCopy.setText("");
    methods.getSelectionModel().clearSelection();
    for (final var input :
        List.of(methodTitle, reportedYield, yieldBasis, procedure, methodNotes)) {
      input.clear();
    }
    changing = false;
    methodDirty = false;
    if (!referenceDirty) {
      expected = editor.document();
    }
    methodDetails.setExpanded(false);
    updateState();
  }

  private void updateState() {
    referenceState.setText(referenceDirty ? "Есть незаписанные поля источника." : "");
    referenceState.setVisible(referenceDirty);
    referenceState.setManaged(referenceDirty);
    methodState.setText(methodDirty ? "Есть незаписанные поля методики." : "");
    methodState.setVisible(methodDirty);
    methodState.setManaged(methodDirty);
    if (toMethod != null) {
      toMethod.setDisable(referenceId == null || referenceDirty);
      reuse.setDisable(methodId == null || methodDirty);
    }
  }

  private void methodForReference() {
    attempt(
        () -> {
          if (referenceDirty || referenceId == null) {
            throw new IllegalArgumentException("Сначала сохраните источник.");
          }
          if (methodDirty) {
            throw new IllegalArgumentException(
                "Сохраните или сбросьте незавершённую методику перед созданием новой.");
          }
          final String selected = referenceId;
          newMethod();
          changing = true;
          methodReference.setValue(
              methodReference.getItems().stream()
                  .filter(r -> r.id().equals(selected))
                  .findFirst()
                  .orElseThrow());
          changing = false;
          tabs.getSelectionModel().select(1);
          updateState();
          status.setText(
              "Источник выбран. Введите название и процедуру, затем сохраните методику.");
        });
  }

  private void sourceAttempt(final Runnable action) {
    attempt(action, referenceDetails);
  }

  private void methodAttempt(final Runnable action) {
    attempt(action, methodDetails);
  }

  private void commit(final LaboratoryLiterature next, final String message) {
    editor.literature(expected, next);
    expected = editor.document();
    refresh();
    save.run();
    status.setText(message);
  }

  private void saveReference() {
    sourceAttempt(
        () -> {
          final String y = year.getText().strip();
          if (y.length() > 4) {
            throw new IllegalArgumentException("Проверьте год публикации.");
          }
          final var reference =
              new Reference(
                  referenceId == null ? "reference_" + UUID.randomUUID() : referenceId,
                  doi.getText(),
                  title.getText(),
                  authors.getText(),
                  y.isEmpty() ? 0 : Integer.parseInt(y),
                  url.getText(),
                  supplement.getText(),
                  attachmentChecks.entrySet().stream()
                      .filter(e -> e.getValue().isSelected())
                      .map(Map.Entry::getKey)
                      .toList(),
                  referenceNotes.getText(),
                  expected.experiment().literature().references().stream()
                      .filter(r -> r.id().equals(referenceId))
                      .map(Reference::copyNote)
                      .findFirst()
                      .orElse(""));
          final var current = expected.experiment().literature();
          final var values = new ArrayList<>(current.references());
          values.removeIf(r -> r.id().equals(reference.id()));
          values.add(reference);
          commit(new LaboratoryLiterature(values, current.methods()), "Источник сохранён в опыте.");
          loadReference(reference);
        });
  }

  private void saveMethod() {
    methodAttempt(
        () -> {
          final var ref =
              Objects.requireNonNull(methodReference.getValue(), "Выберите источник методики.");
          final String value = reportedYield.getText().strip().replace(',', '.');
          if (value.length() > 30) {
            throw new IllegalArgumentException("Проверьте литературный выход.");
          }
          final var method =
              new Method(
                  methodId == null ? "method_" + UUID.randomUUID() : methodId,
                  ref.id(),
                  stage.getValue() == null ? null : stage.getValue().id(),
                  methodTitle.getText(),
                  procedure.getText(),
                  value.isEmpty() ? null : new BigDecimal(value),
                  yieldBasis.getText(),
                  methodNotes.getText(),
                  expected.experiment().literature().methods().stream()
                      .filter(m -> m.id().equals(methodId))
                      .map(Method::copyNote)
                      .findFirst()
                      .orElse(""));
          final var current = expected.experiment().literature();
          final var values = new ArrayList<>(current.methods());
          values.removeIf(m -> m.id().equals(method.id()));
          values.add(method);
          commit(
              new LaboratoryLiterature(current.references(), values),
              "Методика сохранена. Её выход остаётся литературным значением.");
          loadMethod(method);
        });
  }

  private void removeReference() {
    attempt(
        () -> {
          if (referenceId == null) {
            throw new IllegalArgumentException("Выберите сохранённый источник.");
          }
          if (expected.experiment().literature().methods().stream()
              .anyMatch(m -> m.referenceId().equals(referenceId))) {
            throw new IllegalArgumentException(
                "Сначала удалите или перепривяжите методики этого источника.");
          }
          commit(
              new LaboratoryLiterature(
                  expected.experiment().literature().references().stream()
                      .filter(r -> !r.id().equals(referenceId))
                      .toList(),
                  expected.experiment().literature().methods()),
              "Источник удалён; отмена доступна в опыте.");
          newReference();
        });
  }

  private void removeMethod() {
    attempt(
        () -> {
          if (methodId == null) {
            throw new IllegalArgumentException("Выберите сохранённую методику.");
          }
          commit(
              new LaboratoryLiterature(
                  expected.experiment().literature().references(),
                  expected.experiment().literature().methods().stream()
                      .filter(m -> !m.id().equals(methodId))
                      .toList()),
              "Методика удалена; существующий план работы сохранён.");
          newMethod();
        });
  }

  private void reuseMethod() {
    attempt(
        () -> {
          if (methodDirty || methodId == null) {
            throw new IllegalArgumentException(
                "Сначала сохраните методику; затем добавьте её в план.");
          }
          final var steps =
              LaboratoryLiteratureService.appendMethod(
                  expected.experiment(),
                  expected.experiment().literature(),
                  methodId,
                  stage.getValue() == null ? null : stage.getValue().id());
          editor.procedurePlan(expected, steps);
          expected = editor.document();
          save.run();
          status.setText(
              "Методика добавлена в план работы. Текст и источник перенесены; количества и выполнение задайте явно.");
        });
  }

  private void lookup() {
    attempt(
        () -> {
          final String requested = LaboratoryLiterature.normalizeDoi(doi.getText());
          if (requested.isBlank()) {
            throw new IllegalArgumentException("Введите DOI.");
          }
          final var snapshot = new ArrayList<String>();
          for (final var input :
              List.of(doi, title, authors, year, url, supplement, referenceNotes)) {
            snapshot.add(input.getText());
          }
          busy(true);
          fetch.setDisable(true);
          status.setText("Получаю библиографические сведения Crossref…");
          worker =
              Thread.ofVirtual()
                  .start(
                      () -> {
                        try {
                          final var result = new LaboratoryCrossrefClient().lookup(requested);
                          Platform.runLater(
                              () -> {
                                if (disposed) {
                                  return;
                                }
                                busy(false);
                                fetch.setDisable(false);
                                final var now = new ArrayList<String>();
                                for (final var input :
                                    List.of(
                                        doi,
                                        title,
                                        authors,
                                        year,
                                        url,
                                        supplement,
                                        referenceNotes)) {
                                  now.add(input.getText());
                                }
                                if (!snapshot.equals(now)) {
                                  status.setText(
                                      "Поля изменены во время запроса. Ответ не заменил ваш ввод; повторите поиск при необходимости.");
                                  return;
                                }
                                changing = true;
                                doi.setText(result.doi());
                                title.setText(result.title());
                                authors.setText(result.authors());
                                year.setText(
                                    result.year() == 0 ? "" : Integer.toString(result.year()));
                                if (url.getText().isBlank()) {
                                  url.setText(result.url());
                                }
                                referenceNotes.setText(
                                    (referenceNotes.getText().isBlank()
                                            ? ""
                                            : referenceNotes.getText() + "\n")
                                        + result.notes());
                                changing = false;
                                referenceDirty = true;
                                updateState();
                                status.setText(
                                    "Метаданные получены. Проверьте название и авторов, затем сохраните источник.");
                              });
                        } catch (final Exception exception) {
                          Platform.runLater(
                              () -> {
                                if (!disposed) {
                                  busy(false);
                                  fetch.setDisable(false);
                                  status.setText(
                                      Objects.toString(
                                          exception.getMessage(), "Запрос Crossref остановлен."));
                                }
                              });
                        }
                      });
        });
  }

  private void refreshArticleLinks() {
    this.sciHub
        .getItems()
        .setAll(
            this.articleLinks.mirrors().stream()
                .map(
                    address ->
                        menu(
                            address,
                            () ->
                                attempt(
                                    () ->
                                        openLink(
                                            LaboratoryArticleLinks.article(address, doi.getText())
                                                .toString()))))
                .toList());
  }

  private void saveArticleLinks() {
    sourceAttempt(
        () -> {
          this.articleLinks.saveMirrors(this.mirrorAddresses.getText());
          this.mirrorAddresses.setText(String.join("\n", this.articleLinks.mirrors()));
          this.refreshArticleLinks();
          this.status.setText("Адреса сохранены в локальных настройках. Доступность не проверена.");
        });
  }

  private String articleLink() {
    if (!url.getText().isBlank()) {
      return url.getText().strip();
    }
    final String identifier = LaboratoryLiterature.normalizeDoi(doi.getText());
    if (identifier.isBlank()) {
      throw new IllegalArgumentException(
          "Введите DOI или ссылку «Статья · URL» в подробностях источника.");
    }
    return "https://doi.org/" + identifier;
  }

  private void openArticle() {
    sourceAttempt(() -> openLink(articleLink()));
  }

  private void openLink(final String raw) {
    attempt(
        () -> {
          final var uri = java.net.URI.create(raw.strip());
          if (!("https".equalsIgnoreCase(uri.getScheme())
                  || "http".equalsIgnoreCase(uri.getScheme()))
              || uri.getHost() == null
              || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Укажите ссылку HTTP или HTTPS.");
          }
          if (this.linkOpener != null) {
            this.linkOpener.accept(uri);
            return;
          }
          if (!java.awt.Desktop.isDesktopSupported()) {
            throw new IllegalArgumentException(
                "Системный браузер недоступен. Скопируйте ссылку из поля.");
          }
          Thread.ofVirtual()
              .start(
                  () -> {
                    try {
                      java.awt.Desktop.getDesktop().browse(uri);
                    } catch (final Exception exception) {
                      Platform.runLater(
                          () ->
                              status.setText(
                                  "Не удалось открыть браузер; скопируйте ссылку из поля."));
                    }
                  });
        });
  }

  private void attempt(final Runnable action) {
    attempt(action, null);
  }

  private void attempt(final Runnable action, final TitledPane reveal) {
    if (running) {
      status.setText("Дождитесь завершения запроса.");
      return;
    }
    try {
      action.run();
    } catch (final RuntimeException exception) {
      if (reveal != null) {
        reveal.setExpanded(true);
      }
      status.setText(Objects.toString(exception.getMessage(), "Проверьте введённые данные."));
      updateState();
    }
  }

  boolean pending() {
    return running || referenceDirty || methodDirty;
  }

  @Override
  boolean canClose() {
    if (!super.canClose()) {
      return false;
    }
    if (referenceDirty || methodDirty) {
      status.setText(
          "Есть незавершённые поля. Сохраните источник / методику или выберите сброс полей в меню «Ещё» соответствующей вкладки.");
      return false;
    }
    return true;
  }

  @Override
  void dispose() {
    super.dispose();
    if (worker != null) {
      worker.interrupt();
    }
  }

  void draft(final Properties data, final boolean restore) {
    final var fields = new LinkedHashMap<String, Control>();
    fields.put("doi", doi);
    fields.put("title", title);
    fields.put("authors", authors);
    fields.put("year", year);
    fields.put("url", url);
    fields.put("supplement", supplement);
    fields.put("referenceNotes", referenceNotes);
    fields.put("methodTitle", methodTitle);
    fields.put("yield", reportedYield);
    fields.put("basis", yieldBasis);
    fields.put("procedure", procedure);
    fields.put("methodNotes", methodNotes);
    changing = true;
    LaboratoryFormDraft.fields(data, "literature", restore, fields);
    if (restore) {
      referenceId = nullable(data.getProperty("literature.referenceId"));
      methodId = nullable(data.getProperty("literature.methodId"));
      referenceDirty = Boolean.parseBoolean(data.getProperty("literature.referenceDirty"));
      methodDirty = Boolean.parseBoolean(data.getProperty("literature.methodDirty"));
      final String ref = data.getProperty("literature.methodReference", ""),
          stageId = data.getProperty("literature.stage", "");
      methodReference.setValue(
          methodReference.getItems().stream()
              .filter(r -> r.id().equals(ref))
              .findFirst()
              .orElse(null));
      stage.setValue(
          stage.getItems().stream()
              .filter(s -> Objects.equals(Objects.toString(s.id(), ""), stageId))
              .findFirst()
              .orElse(stage.getItems().getFirst()));
      final var selected = Set.of(data.getProperty("literature.attachments", "").split(","));
      attachmentChecks.forEach((id, check) -> check.setSelected(selected.contains(id)));
      tabs.getSelectionModel().select("1".equals(data.getProperty("literature.tab")) ? 1 : 0);
      referenceDetails.setExpanded(
          Boolean.parseBoolean(data.getProperty("literature.referenceDetails", "false")));
      methodDetails.setExpanded(
          Boolean.parseBoolean(data.getProperty("literature.methodDetails", "false")));
    } else {
      data.setProperty("literature.referenceId", Objects.toString(referenceId, ""));
      data.setProperty("literature.methodId", Objects.toString(methodId, ""));
      data.setProperty("literature.referenceDirty", Boolean.toString(referenceDirty));
      data.setProperty("literature.methodDirty", Boolean.toString(methodDirty));
      data.setProperty(
          "literature.methodReference",
          methodReference.getValue() == null ? "" : methodReference.getValue().id());
      data.setProperty(
          "literature.stage",
          stage.getValue() == null ? "" : Objects.toString(stage.getValue().id(), ""));
      data.setProperty(
          "literature.attachments",
          String.join(
              ",",
              attachmentChecks.entrySet().stream()
                  .filter(e -> e.getValue().isSelected())
                  .map(Map.Entry::getKey)
                  .toList()));
      data.setProperty(
          "literature.tab", Integer.toString(tabs.getSelectionModel().getSelectedIndex()));
      data.setProperty(
          "literature.referenceDetails", Boolean.toString(referenceDetails.isExpanded()));
      data.setProperty("literature.methodDetails", Boolean.toString(methodDetails.isExpanded()));
    }
    changing = false;
    updateState();
  }

  private static Label cellNote(final String text, final ListCell<?> cell) {
    final var label = note(text);
    label.maxWidthProperty().bind(cell.widthProperty().subtract(18));
    return label;
  }

  private static ScrollPane formScroll(final javafx.scene.Node node) {
    final var pane = scroll(node);
    pane.setFitToHeight(false);
    return pane;
  }

  private static String nullable(final String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static VBox field(final String label, final Node value) {
    final var box = new VBox(4, note(label), value);
    box.setMaxWidth(Double.MAX_VALUE);
    if (value instanceof Region region) {
      region.setMaxWidth(Double.MAX_VALUE);
    }
    HBox.setHgrow(box, Priority.ALWAYS);
    return box;
  }

  private static MenuItem menu(final String title, final Runnable action) {
    final var item = new MenuItem(title);
    item.setOnAction(e -> action.run());
    return item;
  }

  private static MenuButton more(final MenuItem... items) {
    final var button = new MenuButton("Ещё");
    button.getItems().setAll(items);
    return button;
  }

  private static Button button(final String title, final Runnable action) {
    final var button = new Button(title);
    button.setOnAction(e -> action.run());
    return button;
  }
}
