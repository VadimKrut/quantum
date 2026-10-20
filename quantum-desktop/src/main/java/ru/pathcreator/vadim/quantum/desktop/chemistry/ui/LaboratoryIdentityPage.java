/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryIdentityService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeSkeletonHydrogens;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.CdkCompoundIdentifiers;
import ru.pathcreator.vadim.quantum.desktop.chemistry.infrastructure.PubChemIdentityClient;

/** Names, structure and provenance together; all edits share the editor's undo and recoverable-draft lifecycle. */
final class LaboratoryIdentityPage extends LaboratoryStructurePage {
  private final Map<String, TextInputControl> fields = new LinkedHashMap<>();
  private final Map<String, VBox> fieldRows = new LinkedHashMap<>();
  private final MenuButton copies = new MenuButton("Копировать");
  private final TitledPane otherNames = new TitledPane(),
      identifiersPane = new TitledPane(),
      searchPane = new TitledPane(),
      namingPane = new TitledPane(),
      actionsPane = new TitledPane();
  private final TextField query = new TextField();
  private final Canvas structure = new Canvas(460, 250);
  private final VBox form = new VBox(12);
  private final VBox summary = new VBox(9);
  private final HBox columns = new HBox(22);
  private final VBox vertical = new VBox(14);
  private final StackPane body = new StackPane();
  private final TitledPane compactSummary = new TitledPane();
  private final Button previous = new Button("Вернуть сведения до поиска");
  private Map<String, String> previousValues;
  private MolecularIdentity previousLoaded;
  private Map<String, String> previousLoadedValues;
  private boolean previousStale;
  private final Label provenance = note("");
  private final Label searchState = note("");
  private final Button lookup = new Button("Найти IUPAC по структуре");
  private final Button useIupac = new Button("Использовать IUPAC как название");
  private final Button byName = new Button("Проверить название / CID");
  private final Button cancel = new Button("Остановить поиск");
  private final CheckBox reviewed = new CheckBox("Проверено для текущей структуры");
  private final CheckBox clear = new CheckBox("Удалить сохранённую карточку");
  private final PubChemIdentityClient client;
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread t = new Thread(r, "compound-identity");
            t.setDaemon(true);
            return t;
          });
  private Future<?> job;
  private int generation;
  private boolean busy;
  private boolean stale;
  private boolean lookupAllowed;
  private MolecularIdentity loaded;
  private Map<String, String> loadedValues = Map.of();
  private Boolean compactLayout;

  LaboratoryIdentityPage(
      final LaboratoryEditor editor, final String component, final Runnable close) {
    this(editor, component, close, new PubChemIdentityClient());
  }

  LaboratoryIdentityPage(
      final LaboratoryEditor editor,
      final String component,
      final Runnable close,
      final PubChemIdentityClient client) {
    super(editor, component, "identity", "Карточка вещества", close);
    this.client = client;
    this.setId("chemistry-identity-page");
    this.apply.setText("Применить карточку");
    this.apply.getStyleClass().add("laboratory-primary-action");
    ((FlowPane) this.getTop())
        .getChildren().stream()
            .filter(Label.class::isInstance)
            .map(Label.class::cast)
            .forEach(label -> label.setText("Карточка вещества"));
    this.compactSummary.setText("Структура вещества");
    this.compactSummary.setExpanded(false);
    this.compactSummary.setAnimated(false);
    this.compactSummary.setMinHeight(Region.USE_PREF_SIZE);
    final var molecule = this.source.molecule();
    final Label title = LaboratoryComponentNaming.label(molecule.displayName());
    title.getStyleClass().add("laboratory-navigation-title");
    final Label formula = note(molecule.formula().hillNotation());
    formula.setStyle("-fx-font-size: 18px;");
    final String mass =
        String.format(
            java.util.Locale.ROOT, "%.3f", molecule.composition().mass().averageAtomicMass());
    LaboratoryPreviewTheme.track(this.structure, this::paint);
    this.structure.setId("chemistry-identity-structure");
    this.structure.widthProperty().addListener(o -> this.paint());
    final StackPane picture = new StackPane(this.structure);
    picture.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 10;");
    picture.setMinSize(0, 250);
    picture.setMaxWidth(Double.MAX_VALUE);
    this.structure.widthProperty().bind(picture.widthProperty());
    final var clip = new javafx.scene.shape.Rectangle();
    clip.widthProperty().bind(picture.widthProperty());
    clip.heightProperty().bind(picture.heightProperty());
    picture.setClip(clip);
    final Label composition = note("≈ " + mass + " г/моль · по структуре");
    composition.setTooltip(
        new Tooltip(
            "Формула и молярная масса рассчитаны по явным атомам. Плотность, чистота и концентрация задаются отдельно в количествах."));
    this.summary.getChildren().setAll(title, formula, composition, picture, this.provenance);
    this.summary.setMinWidth(0);
    this.summary.setPrefWidth(340);
    this.summary.setMaxWidth(380);
    this.form.setMinWidth(0);
    this.form.setPrefWidth(680);
    this.form.setMaxWidth(820);
    HBox.setHgrow(this.form, Priority.ALWAYS);
    this.columns.setMaxWidth(1220);
    this.copies.setId("chemistry-identity-copy-menu");
    this.addField("common", "Название вещества", false, "Принятое или введённое вручную название");
    this.addField(
        "caption", "Название на схеме", false, "Своё название, например: исходник A или 5b");
    this.fields.get("caption").setText(molecule.displayName());
    this.fields
        .get("caption")
        .setTooltip(
            new Tooltip(
                "Короткая подпись на схеме. Химические названия и название опыта сохраняются отдельно."));
    this.addField(
        "iupac",
        "Систематическое название · IUPAC",
        true,
        "Введите из проверенного источника или найдите в PubChem");
    this.addField(
        "synonyms",
        "Синонимы · по одному на строку",
        true,
        "Названия, обозначения и номера из источника");
    this.addField(
        "cas",
        "CAS · проверяется контрольная цифра",
        false,
        "Не извлекается автоматически из синонимов");
    this.addField("smiles", "SMILES из источника", true, "");
    this.addField("inchi", "InChI из источника", true, "");
    this.addField("key", "InChIKey", false, "");
    this.addField("cid", "PubChem CID", false, "");
    this.addField(
        "source",
        "Источник названий",
        true,
        "Статья, DOI, справочник или основание ручного названия");
    final FlowPane captions =
        new FlowPane(8, 6, button("Взять подпись из названия", () -> this.caption("common")));
    this.section(
        this.otherNames,
        "Синонимы и варианты названия",
        new VBox(10, this.fieldRows.get("synonyms"), captions),
        "chemistry-identity-other-names");
    this.section(
        this.identifiersPane,
        "CAS и структурные идентификаторы",
        new VBox(
            9,
            this.fieldRows.get("cas"),
            this.fieldRows.get("smiles"),
            this.fieldRows.get("inchi"),
            this.fieldRows.get("key"),
            this.fieldRows.get("cid")),
        "chemistry-identity-identifiers");
    this.section(
        this.actionsPane,
        "Дополнительные действия",
        new VBox(8, this.clear),
        "chemistry-identity-actions");
    this.clear
        .selectedProperty()
        .addListener(
            o -> {
              this.actionsPane.setText(
                  this.clear.isSelected()
                      ? "Выбрано удаление сохранённой карточки"
                      : "Дополнительные действия");
              if (this.clear.isSelected()) {
                this.actionsPane.setExpanded(true);
              }
            });
    this.reviewed.setText("Названия проверены для текущей структуры");
    this.reviewed.setWrapText(true);
    this.reviewed.setVisible(false);
    this.reviewed.setManaged(false);

    String local = "";
    String explanation;
    try {
      final var result = new CdkCompoundIdentifiers().describe(this.source);
      local = result.smiles();
      explanation = result.explanation();
      this.lookupAllowed = result.identityLookupAllowed();
    } catch (final RuntimeException e) {
      explanation = message(e);
    }
    if (MoleculeSkeletonHydrogens.managed(this.source.molecule())
        && MoleculeSkeletonHydrogens.unsupportedCount(this.source) > 0) {
      this.lookupAllowed = false;
      explanation =
          "Сначала проверьте H, заряд и валентность в редакторе структуры. "
              + String.join(" ", MoleculeSkeletonHydrogens.diagnostics(this.source));
    }
    final TextArea localSmiles = new TextArea(local);
    localSmiles.setId("chemistry-identity-local-smiles");
    localSmiles.setEditable(false);
    localSmiles.setWrapText(true);
    localSmiles.setPrefRowCount(2);
    localSmiles.setMaxHeight(80);
    final Button copyLocal =
        button("Копировать локальный SMILES", () -> this.copy(localSmiles.getText()));
    copyLocal.disableProperty().bind(localSmiles.textProperty().isEmpty());
    final VBox localIdentifiers =
        new VBox(8, note("SMILES, рассчитанный из текущей структуры"), localSmiles, copyLocal);
    this.query.setPromptText("Название на языке источника или CID");
    this.query.setId("chemistry-identity-query");
    this.lookup.setId("chemistry-identity-lookup");
    this.byName.setId("chemistry-identity-by-name");
    this.cancel.setId("chemistry-identity-stop");
    this.lookup.setOnAction(e -> this.findIupac());
    this.byName.setOnAction(e -> this.lookup(false));
    this.query.setOnAction(e -> this.lookup(false));
    this.cancel.setOnAction(e -> this.stop());
    this.searchState.setId("chemistry-identity-search-status");
    final TitledPane searchDetails = new TitledPane();
    this.section(
        searchDetails,
        "Как проверяется результат",
        new VBox(
            8,
            note(
                "Граф и заданная конфигурация проверяются локально. Для нового вещества без справочной записи имя вводится вручную с источником. Ручные названия не становятся автоматически подтверждёнными IUPAC-названиями."),
            note(explanation),
            localIdentifiers),
        "chemistry-identity-search-details");

    this.useIupac.setId("chemistry-identity-use-iupac");
    this.useIupac
        .disableProperty()
        .bind(
            this.fields
                .get("iupac")
                .textProperty()
                .isEmpty()
                .or(this.fields.get("iupac").disabledProperty()));
    this.useIupac.setTooltip(
        new Tooltip(
            "Предложить IUPAC как подпись выбранной структуры. Изменение запишется после «Применить карточку»."));
    this.useIupac.setOnAction(
        event -> {
          this.caption("iupac");
          this.error.setText(
              "Название предложено в поле «Подпись на схеме». Нажмите «Применить карточку», чтобы записать его.");
        });
    final VBox naming =
        new VBox(
            8,
            new FlowPane(8, 6, this.lookup, this.cancel),
            note(
                "В PubChem отправляется только SMILES этого вещества. Результат проверяется по структуре."),
            this.searchState,
            this.previous,
            this.fieldRows.get("iupac"),
            this.useIupac);
    this.section(
        this.namingPane,
        "IUPAC и поиск в PubChem · необязательно",
        naming,
        "chemistry-identity-optional-iupac");
    this.form
        .getChildren()
        .setAll(
            this.fieldRows.get("caption"),
            note("Своё название можно сохранить без интернета. Поиск IUPAC — по желанию."),
            this.namingPane,
            this.fieldRows.get("common"),
            this.fieldRows.get("source"),
            this.reviewed,
            this.otherNames,
            this.identifiersPane,
            this.actionsPane);
    final VBox search =
        new VBox(
            8,
            note("Здесь в PubChem отправляется только введённый запрос."),
            this.query,
            this.byName,
            searchDetails);
    this.section(
        this.searchPane, "Другой поиск · название или CID", search, "chemistry-identity-pubchem");
    this.summary.getChildren().add(this.searchPane);
    if (!this.lookupAllowed) {
      this.searchState.setText("Поиск IUPAC для этой структуры недоступен: " + explanation);
    }
    this.previous.setVisible(false);
    this.previous.setManaged(false);
    this.previous.setId("chemistry-identity-previous");
    this.previous.setOnAction(event -> this.restorePrevious());
    try {
      this.loaded = MolecularIdentity.read(molecule).orElse(null);
      if (this.loaded != null) {
        this.fill(this.loaded);
        this.stale = !new CdkCompoundIdentifiers().current(this.source, this.loaded);
      }
    } catch (final RuntimeException e) {
      this.error.setText(message(e));
      this.stale = true;
    }
    this.provenance.setText(
        this.loaded == null
            ? "Химическое название ещё не записано."
            : this.originText(this.loaded));
    this.reviewed.setVisible(this.stale);
    this.reviewed.setManaged(this.stale);
    if (this.stale) {
      this.provenance.setText(
          "Сведения требуют проверки: структура или SMILES изменились.\n"
              + this.provenance.getText());
    }
    this.clear.setId("chemistry-identity-clear");
    this.reviewed.setId("chemistry-identity-reviewed");
    this.loadedValues = this.values();
    this.seal();
    this.content(this.body);
    this.widthProperty().addListener(o -> this.layoutColumns());
    this.layoutColumns();
    this.controls(false);
    this.paint();
    this.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
            this.apply.fire();
            event.consume();
          }
        });
  }

  private String originText(final MolecularIdentity identity) {
    this.provenance.setTooltip(
        new Tooltip(
            "Источник: "
                + identity.source()
                + "\nВремя записи: "
                + identity.recordedAt()
                + "\n"
                + (identity.origin() == MolecularIdentity.Origin.PUBCHEM
                    ? "Проверено соответствие сохранённой структуре."
                    : "Ручное название: химическое соответствие проверяет автор.")));
    return (identity.origin() == MolecularIdentity.Origin.PUBCHEM
            ? "Сведения из PubChem"
            : "Названия с указанными источниками")
        + " · "
        + java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(identity.recordedAt());
  }

  private void section(
      final TitledPane pane, final String title, final javafx.scene.Node content, final String id) {
    pane.setText(title);
    pane.setContent(content);
    pane.setId(id);
    pane.setExpanded(false);
    pane.setAnimated(false);
    pane.setMinHeight(Region.USE_PREF_SIZE);
  }

  private void addField(
      final String key, final String label, final boolean multiline, final String prompt) {
    final TextInputControl field = multiline ? new TextArea() : new TextField();
    field.setId("chemistry-identity-" + key);
    field.setPromptText(prompt);
    field.setMaxWidth(Double.MAX_VALUE);
    if (field instanceof TextArea area) {
      if (!Set.of("smiles", "inchi").contains(key)) {
        area.setStyle("-fx-font-family: System; -fx-font-size: 13px;");
      }
      area.setWrapText(true);
      area.setPrefRowCount(key.equals("synonyms") ? 3 : 2);
      area.setMaxHeight(key.equals("synonyms") ? 110 : 80);
    }
    this.fields.put(key, field);
    final MenuItem copy = new MenuItem(label);
    copy.setOnAction(e -> this.copy(field.getText()));
    copy.disableProperty().bind(field.textProperty().isEmpty());
    this.copies.getItems().add(copy);
    final Label caption = note(label);
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final HBox heading = new HBox(8, caption, spacer);
    if (key.equals("common")) {
      heading.getChildren().add(this.copies);
    }
    heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    this.fieldRows.put(key, new VBox(5, heading, field));
  }

  private void copy(final String value) {
    if (value.isBlank()) {
      return;
    }
    final ClipboardContent content = new ClipboardContent();
    content.putString(value);
    Clipboard.getSystemClipboard().setContent(content);
    this.error.setText("Скопировано.");
  }

  private void caption(final String key) {
    if (this.fields.get(key).getText().isBlank()) {
      this.error.setText("Сначала укажите название.");
      return;
    }
    this.fields.get("caption").setText(this.fields.get(key).getText());
  }

  private void layoutColumns() {
    final boolean compact = this.getWidth() < 800;
    if (Objects.equals(this.compactLayout, compact)) {
      return;
    }
    this.compactLayout = compact;
    this.compactSummary.setContent(null);
    this.columns.getChildren().clear();
    this.vertical.getChildren().clear();
    this.body.getChildren().clear();
    if (compact) {
      this.compactSummary.setContent(this.summary);
      this.vertical.getChildren().setAll(this.compactSummary, this.form);
      this.body.getChildren().setAll(this.vertical);
    } else {
      this.summary.setVisible(true);
      this.summary.setManaged(true);
      this.columns.getChildren().setAll(this.summary, this.form);
      this.body.getChildren().setAll(this.columns);
    }
    this.body.setPadding(new Insets(14, 20, 20, 20));
  }

  private void paint() {
    final var g = this.structure.getGraphicsContext2D();
    final double w = this.structure.getWidth(), h = this.structure.getHeight();
    LaboratoryPreviewTheme.clear(g);
    final var b = LaboratoryCanvas.previewBounds(this.source);
    final double scale =
        Math.min(
            1.6,
            Math.min((w - 28) / Math.max(1, b.getWidth()), (h - 24) / Math.max(1, b.getHeight())));
    g.save();
    g.translate(
        w / 2 - (b.getMinX() + b.getWidth() / 2) * scale,
        h / 2 - (b.getMinY() + b.getHeight() / 2) * scale);
    g.scale(scale, scale);
    LaboratoryPreviewTheme.paint(g, this.source);
    g.restore();
  }

  private void fill(final MolecularIdentity identity) {
    final Map<String, String> data =
        Map.ofEntries(
            Map.entry("common", identity.commonName()),
            Map.entry("iupac", identity.iupacName()),
            Map.entry("synonyms", String.join("\n", identity.synonyms())),
            Map.entry("cas", identity.cas()),
            Map.entry("smiles", identity.smiles()),
            Map.entry("inchi", identity.inchi()),
            Map.entry("key", identity.inchiKey()),
            Map.entry("cid", identity.pubchemCid()),
            Map.entry("source", identity.source()));
    data.forEach((k, v) -> this.fields.get(k).setText(v));
  }

  private void controls(final boolean busy) {
    this.busy = busy;
    this.fieldRows.values().forEach(row -> row.setDisable(busy));
    this.reviewed.setDisable(busy);
    this.clear.setDisable(busy);
    this.apply.setDisable(busy);
    this.query.setDisable(busy);
    this.lookup.setDisable(busy || !this.lookupAllowed);
    this.byName.setDisable(busy || !this.lookupAllowed);
    this.cancel.setVisible(busy);
    this.cancel.setManaged(busy);
    this.previous.setDisable(busy);
  }

  /** Only an explicit drawing-toolbar or page-button action invokes a public-database request. */
  void findIupac() {
    this.namingPane.setExpanded(true);
    this.lookup(true);
  }

  private void lookup(final boolean structure) {
    this.namingPane.setExpanded(true);
    if (this.disposed || this.busy || !this.lookupAllowed) {
      return;
    }
    if (!this.current()) {
      this.searchState.setText("Структура изменилась. Откройте карточку заново.");
      return;
    }
    this.previousValues = this.values();
    this.previousLoaded = this.loaded;
    this.previousLoadedValues = this.loadedValues;
    this.previousStale = this.stale;
    final String text = this.query.getText();
    final int token = ++this.generation;
    this.controls(true);
    this.searchState.setText("Поиск в PubChem и проверка структуры…");
    this.job =
        this.worker.submit(
            () -> {
              try {
                final var result = this.client.lookup(this.source, text, structure);
                Platform.runLater(
                    () -> {
                      if (this.disposed || token != this.generation) {
                        return;
                      }
                      this.controls(false);
                      if (!this.current()) {
                        this.searchState.setText(
                            "Исходная структура изменилась; результат не применён.");
                        return;
                      }
                      try {
                        final var merged =
                            LaboratoryIdentityService.preserveEnteredNames(
                                this.source,
                                result,
                                this.previousValues.get("common"),
                                Arrays.asList(this.previousValues.get("synonyms").split("\n")),
                                this.previousValues.get("source"));
                        this.fill(merged);
                        if (this.fields
                                .get("caption")
                                .getText()
                                .equals(this.source.molecule().displayName())
                            && this.source
                                .molecule()
                                .displayName()
                                .matches("Структура\\s+[0-9]+")) {
                          this.fields.get("caption").setText(result.iupacName());
                        }
                        this.previous.setVisible(true);
                        this.previous.setManaged(true);
                        this.loaded = merged;
                        this.stale = false;
                        this.reviewed.setSelected(false);
                        this.reviewed.setVisible(false);
                        this.reviewed.setManaged(false);
                        this.clear.setSelected(false);
                        this.loadedValues = this.values();
                        this.provenance.setText(this.originText(merged));
                        this.searchState.setText(
                            "IUPAC найдено в PubChem · CID "
                                + result.pubchemCid()
                                + ". Структура совпадает. Проверьте подпись и нажмите «Применить карточку». Своё название можно сохранить.");
                      } catch (final RuntimeException invalid) {
                        this.searchState.setText(message(invalid));
                      }
                    });
              } catch (final Exception e) {
                Platform.runLater(
                    () -> {
                      if (!this.disposed && token == this.generation) {
                        this.controls(false);
                        this.searchState.setText(
                            e instanceof java.net.http.HttpTimeoutException
                                ? "PubChem не ответил вовремя. Повторите запрос."
                                : Objects.requireNonNullElse(
                                    e.getMessage(), "Не удалось получить данные PubChem."));
                      }
                    });
              }
            });
  }

  private void restorePrevious() {
    if (this.busy || this.previousValues == null) {
      return;
    }
    this.fields.forEach((k, v) -> v.setText(this.previousValues.get(k)));
    this.clear.setSelected(Boolean.parseBoolean(this.previousValues.get("clear")));
    this.reviewed.setSelected(Boolean.parseBoolean(this.previousValues.get("reviewed")));
    this.loaded = this.previousLoaded;
    this.loadedValues = this.previousLoadedValues;
    this.stale = this.previousStale;
    this.reviewed.setVisible(this.stale);
    this.reviewed.setManaged(this.stale);
    this.provenance.setText(
        this.loaded == null
            ? "Названия внесены в форму; примените карточку для сохранения в опыте."
            : (this.stale ? "Структура изменилась; сведения требуют проверки.\n" : "")
                + this.originText(this.loaded));
    this.previousValues = null;
    this.previous.setVisible(false);
    this.previous.setManaged(false);
    this.searchState.setText("Возвращены сведения, которые были в форме до поиска.");
  }

  private void stop() {
    this.generation++;
    if (this.job != null) {
      this.job.cancel(true);
    }
    this.controls(false);
    this.searchState.setText("Поиск остановлен. Введённые сведения сохранены в форме.");
  }

  @Override
  protected Map<String, String> values() {
    final Map<String, String> data = new LinkedHashMap<>();
    this.fields.forEach((k, v) -> data.put(k, v.getText()));
    data.put("reviewed", Boolean.toString(this.reviewed.isSelected()));
    data.put("clear", Boolean.toString(this.clear.isSelected()));
    return Map.copyOf(data);
  }

  @Override
  protected void restoreValues(final Map<String, String> values) {
    if (!values.keySet().equals(this.values().keySet())
        || !Set.of("true", "false").contains(values.get("reviewed"))
        || !Set.of("true", "false").contains(values.get("clear"))) {
      throw new IllegalArgumentException("Повреждён черновик карточки вещества.");
    }
    this.fields.forEach((k, v) -> v.setText(values.get(k)));
    this.clear.setSelected(Boolean.parseBoolean(values.get("clear")));
    this.reviewed.setSelected(Boolean.parseBoolean(values.get("reviewed")));
    // Restored editable values are manual claims, not a fresh verified external response.
    this.loaded = null;
    this.loadedValues = Map.of();
  }

  @Override
  protected MoleculeDrawing build() {
    if (this.busy) {
      throw new IllegalArgumentException("Дождитесь завершения поиска или остановите его.");
    }
    final var v = this.values();
    final boolean empty =
        this.fields.entrySet().stream()
            .filter(e -> !Set.of("caption", "source", "cid").contains(e.getKey()))
            .allMatch(e -> e.getValue().getText().isBlank());
    MolecularIdentity identity = null;
    if (!this.clear.isSelected() && !empty) {
      if (this.stale && !this.reviewed.isSelected()) {
        throw new IllegalArgumentException(
            "Проверьте названия для изменённой структуры и отметьте подтверждение.");
      }
      try {
        LaboratoryIdentityService.requireConsistentSmiles(this.source, v.get("smiles"));
      } catch (final RuntimeException invalid) {
        this.identifiersPane.setExpanded(true);
        this.fields.get("smiles").requestFocus();
        throw invalid;
      }
      final boolean unchanged =
          this.loaded != null
              && new CdkCompoundIdentifiers().current(this.source, this.loaded)
              && this.fields.keySet().stream()
                  .filter(k -> !k.equals("caption"))
                  .allMatch(k -> Objects.equals(v.get(k), this.loadedValues.get(k)));
      try {
        identity =
            unchanged
                ? this.loaded
                : new MolecularIdentity(
                    v.get("common"),
                    v.get("iupac"),
                    Arrays.asList(v.get("synonyms").split("\n")),
                    v.get("cas"),
                    v.get("smiles"),
                    v.get("inchi"),
                    v.get("key"),
                    v.get("cid"),
                    v.get("source"),
                    MolecularIdentity.Origin.MANUAL,
                    Instant.now(),
                    MolecularIdentity.signature(this.source.molecule()));
      } catch (final IllegalArgumentException invalid) {
        this.otherNames.setExpanded(true);
        this.identifiersPane.setExpanded(true);
        throw invalid;
      }
    }
    final var molecule =
        MolecularIdentity.attach(this.source.molecule(), v.get("caption"), identity);
    return MoleculeDrawing.of(
        molecule, this.source.atoms(), this.source.bonds(), this.source.manualFields());
  }

  @Override
  boolean canClose() {
    if (this.busy) {
      this.error.setText("Остановите поиск перед выходом.");
      return false;
    }
    final boolean close = super.canClose();
    if (!close) {
      this.error.setText("Примените карточку или явно отмените ввод.");
    }
    return close;
  }

  @Override
  void dispose() {
    this.generation++;
    if (this.job != null) {
      this.job.cancel(true);
    }
    this.worker.shutdownNow();
    super.dispose();
  }
}
