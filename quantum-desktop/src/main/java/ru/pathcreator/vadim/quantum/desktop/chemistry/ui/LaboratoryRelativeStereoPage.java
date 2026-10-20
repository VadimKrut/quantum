/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import java.time.Instant;
import java.util.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryEditor;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** Explicit relative/racemic assignments, native persistence and one editor transaction per save. */
final class LaboratoryRelativeStereoPage extends LaboratoryComputationPage {
  private record Choice(AtomId id, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private final LaboratoryEditor editor;
  private final String component;
  private LaboratoryDrawing expected;
  private MoleculeDrawing drawing;
  private final ListView<Group> groups = new ListView<>();
  private final ListView<Member> members = new ListView<>();
  private final ComboBox<Kind> kind = new ComboBox<>();
  private final ComboBox<Choice> atom = new ComboBox<>(), anchor = new ComboBox<>();
  private final ComboBox<Descriptor> descriptor = new ComboBox<>();
  private final TextArea source = new TextArea();
  private final Label explanation = note(""), recorded = note(""), state = note("");
  private final CheckBox reviewed =
      new CheckBox("Все сохранённые группы проверены после изменения графа");
  private final LaboratoryStructurePreview preview = new LaboratoryStructurePreview();
  private boolean changing, dirty;
  private String editing;

  LaboratoryRelativeStereoPage(
      final LaboratoryEditor editor,
      final String componentId,
      final AtomId selected,
      final Runnable close) {
    super("Относительная стереохимия и рацемат", close);
    this.editor = editor;
    this.component = componentId;
    this.expected = editor.document();
    this.drawing =
        Objects.requireNonNull(expected.layouts().get(componentId), "Выберите структуру.");
    setId("chemistry-relative-stereo-page");
    kind.getItems().setAll(Kind.values());
    kind.setValue(Kind.RELATIVE);
    descriptor.getItems().setAll(Descriptor.values());
    descriptor.setValue(Descriptor.R_STAR);
    source.setWrapText(true);
    source.setPrefRowCount(3);
    source.setPromptText("Источник: эксперимент, статья, явное ручное назначение…");
    source.setId("relative-source");
    kind.setId("relative-kind");
    atom.setId("relative-atom");
    anchor.setId("relative-anchor");
    descriptor.setId("relative-descriptor");
    members.setId("relative-members");
    groups.setId("relative-groups");
    final List<Choice> choices = new ArrayList<>();
    for (int i = 0; i < drawing.molecule().atoms().size(); i++) {
      final var a = drawing.molecule().atoms().get(i);
      if (!a.element().symbol().value().equals("H")) {
        choices.add(new Choice(a.id(), a.element().symbol().value() + " · атом " + (i + 1)));
      }
    }
    atom.getItems().setAll(choices);
    atom.setValue(
        choices.stream()
            .filter(c -> c.id().equals(selected))
            .findFirst()
            .orElse(choices.isEmpty() ? null : choices.getFirst()));
    atom.setMaxWidth(Double.MAX_VALUE);
    anchor.setMaxWidth(Double.MAX_VALUE);
    kind.setMaxWidth(Double.MAX_VALUE);
    descriptor.setMaxWidth(Double.MAX_VALUE);
    groups.setCellFactory(
        list ->
            new ListCell<>() {
              {
                prefWidthProperty().bind(list.widthProperty().subtract(20));
                setMaxWidth(USE_PREF_SIZE);
                setWrapText(true);
              }

              @Override
              protected void updateItem(final Group g, final boolean empty) {
                super.updateItem(g, empty);
                setText(
                    empty || g == null
                        ? null
                        : g.kind()
                            + "\n"
                            + g.members().stream()
                                .map(m -> label(m.atom()) + " " + m.descriptor())
                                .collect(java.util.stream.Collectors.joining("; ")));
              }
            });
    members.setCellFactory(
        list ->
            new ListCell<>() {
              @Override
              protected void updateItem(final Member m, final boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : label(m.atom()) + " · " + m.descriptor());
              }
            });
    members.setPrefHeight(150);
    members.setMinHeight(110);
    groups.setPrefHeight(140);
    groups.setMinHeight(90);
    groups.setPlaceholder(note("Групп пока нет"));
    groups
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, before, after) -> {
              if (changing || after == null) {
                return;
              }
              if (dirty) {
                changing = true;
                groups.getSelectionModel().select(before);
                changing = false;
                status.setText("Сохраните или сбросьте текущую группу перед выбором другой.");
              } else {
                load(after);
              }
            });
    members
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (o, a, b) -> {
              if (b != null) {
                changing = true;
                select(atom, b.atom());
                descriptor.setValue(b.descriptor());
                changing = false;
                preview.show(drawing, b.atom());
              }
            });
    preview.onPick(id -> select(atom, id));
    preview.hint("Выберите центр на рисунке, назначьте R*/S* и добавьте его в группу.");
    atom.valueProperty()
        .addListener(
            o -> {
              if (atom.getValue() != null) {
                preview.show(drawing, atom.getValue().id());
              }
            });
    kind.valueProperty()
        .addListener(
            o -> {
              explain();
              if (!changing) {
                dirty = true;
                if (kind.getValue() == Kind.UNKNOWN_ABSOLUTE) {
                  descriptor.setValue(Descriptor.UNKNOWN);
                }
              }
            });
    anchor
        .valueProperty()
        .addListener(
            o -> {
              if (!changing) {
                dirty = true;
              }
            });
    source
        .textProperty()
        .addListener(
            o -> {
              if (!changing) {
                dirty = true;
              }
            });
    reviewed
        .selectedProperty()
        .addListener(
            o -> {
              if (!changing) {
                dirty = true;
              }
            });
    final VBox list = new VBox(8, note("Сохранённые группы"), groups);
    VBox.setVgrow(groups, Priority.ALWAYS);
    list.setMinWidth(0);
    list.setMinHeight(0);
    list.setPrefWidth(270);
    final VBox form =
        new VBox(
            10,
            state,
            field("Тип записи", kind),
            explanation,
            preview,
            new FlowPane(
                10,
                8,
                field("Центр", atom),
                field("Относительная метка", descriptor),
                button("Добавить / заменить центр", "member-add", this::putMember)),
            members,
            new FlowPane(
                8, 8, button("Убрать выбранный центр", "member-remove", this::removeMember)),
            field("Опорный центр группы", anchor),
            field("Источник и основание назначения *", source),
            recorded,
            reviewed,
            note(
                "R*/S* задают взаимную конфигурацию представителя, а не абсолютный CIP. Для рацемата явно утверждается равная пара энантиомеров. Клинья и исходные записи не переписываются. Стереогенность выбранных центров проверьте по структуре."));
    form.setMinWidth(0);
    form.setMinHeight(Region.USE_PREF_SIZE);
    form.setPadding(new Insets(12));
    final var formScroll = scroll(form);
    formScroll.setFitToHeight(false);
    final var split = new SplitPane(list, formScroll);
    split.setDividerPositions(.26);
    setCenter(split);
    widthProperty()
        .addListener(
            o -> {
              final boolean narrow = getWidth() < 930;
              final var direction =
                  narrow
                      ? javafx.geometry.Orientation.VERTICAL
                      : javafx.geometry.Orientation.HORIZONTAL;
              if (split.getOrientation() != direction) {
                split.setOrientation(direction);
                split.setDividerPositions(narrow ? .21 : .26);
              }
            });
    final var footer =
        new VBox(
            8,
            status,
            new FlowPane(
                8,
                8,
                button("Сохранить группу", "save", this::save),
                button("Удалить выбранную группу", "remove", this::remove),
                button("Удалить все группы", "clear", this::clear),
                button("Сбросить ввод", "discard", this::reset)));
    footer.setPadding(new Insets(10, 16, 12, 16));
    setBottom(footer);
    refresh();
    explain();
    preview.show(drawing, atom.getValue() == null ? null : atom.getValue().id());
  }

  private static VBox field(final String text, final Control input) {
    final var box = new VBox(4, note(text), input);
    box.setMinWidth(0);
    return box;
  }

  private Button button(final String text, final String id, final Runnable action) {
    final var b = new Button(text);
    b.setId("relative-" + id);
    b.setOnAction(
        e -> {
          try {
            action.run();
          } catch (final RuntimeException ex) {
            status.setText(Objects.toString(ex.getMessage(), "Проверьте ввод."));
          }
        });
    return b;
  }

  private String label(final AtomId id) {
    return atom.getItems().stream()
        .filter(c -> c.id().equals(id))
        .map(Choice::label)
        .findFirst()
        .orElse("Отсутствующий атом " + id.value());
  }

  private static void select(final ComboBox<Choice> control, final AtomId id) {
    control.setValue(
        control.getItems().stream().filter(c -> c.id().equals(id)).findFirst().orElse(null));
  }

  private void explain() {
    explanation.setText(
        kind.getValue() == Kind.RELATIVE
            ? "Выберите не менее двух центров и явно задайте их взаимные R*/S*. Общая инверсия всех меток описывает ту же относительную группу."
            : kind.getValue() == Kind.RACEMIC
                ? "Укажите центры одного представителя пары и источник утверждения о рацемате. Это запись состава материала, не измерение энантиомерного избытка."
                : "Выберите один центр с меткой «Не задано». Взаимная конфигурация с другими центрами этой записью не утверждается.");
  }

  private List<Group> saved() {
    return MolecularRelativeStereo.read(drawing.molecule())
        .map(MolecularRelativeStereo::groups)
        .orElse(List.of());
  }

  private void refresh() {
    changing = true;
    groups.getItems().setAll(saved());
    final var current = MolecularRelativeStereo.read(drawing.molecule());
    final boolean stale = current.isPresent() && !current.get().matches(drawing.molecule());
    reviewed.setSelected(false);
    reviewed.setVisible(stale);
    reviewed.setManaged(stale);
    state.setText(
        stale
            ? "Граф изменился. Группы сохранены как предыдущие назначения; проверьте их все и подтвердите либо удалите."
            : "Назначения сохраняются в QCHEM вместе с источником и временем. Абсолютная конфигурация из них не выводится.");
    changing = false;
  }

  private void load(final Group group) {
    changing = true;
    editing = group.id();
    kind.setValue(group.kind());
    members.getItems().setAll(group.members());
    source.setText(group.source());
    anchors(group.anchor());
    recorded.setText("Сохранено: " + group.recordedAt());
    changing = false;
    dirty = false;
    explain();
  }

  private void anchors(final AtomId requested) {
    final var old =
        requested == null && anchor.getValue() != null ? anchor.getValue().id() : requested;
    anchor
        .getItems()
        .setAll(
            members.getItems().stream().map(m -> new Choice(m.atom(), label(m.atom()))).toList());
    select(anchor, old);
    if (anchor.getValue() == null) {
      anchor.getSelectionModel().selectFirst();
    }
  }

  private void putMember() {
    final var selected =
        Objects.requireNonNull(atom.getValue(), "Выберите центр на рисунке или в списке.");
    final var d =
        kind.getValue() == Kind.UNKNOWN_ABSOLUTE
            ? Descriptor.UNKNOWN
            : Objects.requireNonNull(descriptor.getValue());
    members.getItems().removeIf(m -> m.atom().equals(selected.id()));
    members.getItems().add(new Member(selected.id(), d));
    anchors(null);
    dirty = true;
  }

  private void removeMember() {
    final var member = members.getSelectionModel().getSelectedItem();
    if (member == null) {
      throw new IllegalArgumentException("Выберите центр группы.");
    }
    members.getItems().remove(member);
    anchors(null);
    dirty = true;
  }

  private void requireCurrent() {
    if (!editor.document().equals(expected)) {
      throw new IllegalArgumentException(
          "Опыт изменился. Сохраните черновик и откройте актуальную структуру.");
    }
  }

  private void save() {
    requireCurrent();
    final var old = MolecularRelativeStereo.read(drawing.molecule());
    if (old.isPresent() && !old.get().matches(drawing.molecule()) && !reviewed.isSelected()) {
      throw new IllegalArgumentException("Подтвердите проверку всех старых групп либо удалите их.");
    }
    final var id = editing == null ? "relative_" + UUID.randomUUID() : editing;
    final var group =
        new Group(
            id,
            Objects.requireNonNull(kind.getValue()),
            Objects.requireNonNull(anchor.getValue(), "Выберите опорный центр.").id(),
            List.copyOf(members.getItems()),
            source.getText(),
            Instant.now());
    final var all = new ArrayList<>(saved());
    all.removeIf(g -> g.id().equals(id));
    all.add(group);
    commit(
        new MolecularRelativeStereo(MolecularRelativeStereo.signature(drawing.molecule()), all)
            .apply(drawing.molecule()));
    reset();
    status.setText(
        "Группа сохранена. R*/S* остаются относительными пользовательскими назначениями.");
  }

  private void remove() {
    requireCurrent();
    if (editing == null) {
      throw new IllegalArgumentException("Выберите сохранённую группу.");
    }
    final var all = new ArrayList<>(saved());
    all.removeIf(g -> g.id().equals(editing));
    if (all.isEmpty()) {
      clear();
      return;
    }
    final var old = MolecularRelativeStereo.read(drawing.molecule()).orElseThrow();
    if (!old.matches(drawing.molecule())) {
      throw new IllegalArgumentException(
          "После правки графа сначала проверьте группы либо удалите все устаревшие назначения.");
    }
    commit(new MolecularRelativeStereo(old.structureSignature(), all).apply(drawing.molecule()));
    reset();
    status.setText("Группа удалена; отмена доступна в опыте.");
  }

  private void clear() {
    requireCurrent();
    commit(MolecularRelativeStereo.clear(drawing.molecule()));
    reset();
    status.setText(
        "Относительные группы удалены; исходные стереозаписи и клинья сохранены. Отмена доступна в опыте.");
  }

  private void commit(final Molecule molecule) {
    final var role =
        expected.experiment().components().stream()
            .filter(c -> c.id().equals(component))
            .findFirst()
            .orElseThrow()
            .role();
    editor.replace(
        component,
        MoleculeDrawing.of(molecule, drawing.atoms(), drawing.bonds(), drawing.manualFields()),
        role);
    expected = editor.document();
    drawing = expected.layouts().get(component);
    refresh();
    preview.show(drawing, atom.getValue() == null ? null : atom.getValue().id());
  }

  private void reset() {
    changing = true;
    editing = null;
    groups.getSelectionModel().clearSelection();
    members.getItems().clear();
    anchor.getItems().clear();
    source.clear();
    recorded.setText("");
    reviewed.setSelected(false);
    changing = false;
    dirty = false;
  }

  boolean pending() {
    return dirty;
  }

  @Override
  boolean canClose() {
    if (dirty) {
      status.setText("Сохраните группу или нажмите «Сбросить ввод» перед выходом.");
      return false;
    }
    return true;
  }

  void draft(final Properties data, final boolean restore) {
    if (!restore) {
      data.setProperty("relative.component", component);
      data.setProperty("relative.editing", Objects.toString(editing, ""));
      data.setProperty("relative.kind", kind.getValue().name());
      data.setProperty("relative.source", source.getText());
      data.setProperty(
          "relative.members",
          members.getItems().stream()
              .map(m -> m.atom().value() + ":" + m.descriptor().name())
              .collect(java.util.stream.Collectors.joining(",")));
      data.setProperty(
          "relative.anchor", anchor.getValue() == null ? "" : anchor.getValue().id().value());
      data.setProperty("relative.dirty", Boolean.toString(dirty));
      return;
    }
    if (!component.equals(data.getProperty("relative.component"))) {
      throw new IllegalArgumentException("Черновик относится к другому компоненту.");
    }
    final String raw = data.getProperty("relative.members", "");
    if (raw.length() > 20000) {
      throw new IllegalArgumentException("Слишком большой черновик группы.");
    }
    changing = true;
    try {
      editing = data.getProperty("relative.editing", "");
      if (editing.isEmpty()) {
        editing = null;
      }
      kind.setValue(Kind.valueOf(data.getProperty("relative.kind", Kind.RELATIVE.name())));
      source.setText(data.getProperty("relative.source", ""));
      members.getItems().clear();
      if (!raw.isBlank()) {
        final String[] items = raw.split(",", -1);
        if (items.length > 64) {
          throw new IllegalArgumentException("Слишком много центров в черновике.");
        }
        for (final var item : items) {
          final var fields = item.split(":", -1);
          if (fields.length != 2) {
            throw new IllegalArgumentException("Повреждён черновик группы.");
          }
          members.getItems().add(new Member(AtomId.of(fields[0]), Descriptor.valueOf(fields[1])));
        }
      }
      final String selected = data.getProperty("relative.anchor", "");
      anchors(selected.isEmpty() ? null : AtomId.of(selected));
      dirty = Boolean.parseBoolean(data.getProperty("relative.dirty", "true"));
    } finally {
      changing = false;
    }
    explain();
    status.setText(
        "Восстановлен незавершённый ввод. Проверьте центры и источник перед сохранением.");
  }
}
