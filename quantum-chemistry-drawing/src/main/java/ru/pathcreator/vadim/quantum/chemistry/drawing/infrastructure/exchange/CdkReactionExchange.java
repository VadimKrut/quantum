/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.io.*;
import java.util.*;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.io.*;
import org.openscience.cdk.silent.*;
import org.openscience.cdk.smiles.*;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;

/** A bounded reaction envelope around the shared conservative molecular exchange. */
public final class CdkReactionExchange implements ReactionExchange {
  private static void require(final boolean ok, final String message) {
    if (!ok) {
      throw new IllegalArgumentException(message);
    }
  }

  private static void cancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
  }

  private record Graph(IAtomContainer molecule, ReactionComponentRole role) {}

  @Override
  public Entry read(final String input, final Format requested, final String origin) {
    require(
        input != null && !input.isBlank() && input.length() <= CdkStructureExchange.MAX_TEXT,
        "Введите одну реакцию; предел — 4 млн символов.");
    final String text = input.replace("\r\n", "\n").replace('\r', '\n').replaceFirst("^\uFEFF", "");
    final Format format =
        requested == Format.AUTO
            ? text.startsWith("$RXN V3000")
                ? Format.RXN_V3000
                : text.startsWith("$RXN") ? Format.RXN_V2000 : Format.SMILES
            : requested;
    final List<Graph> graphs = new ArrayList<>();
    String title = "Импортированная реакция";
    try {
      if (format == Format.SMILES) {
        require(
            text.strip().lines().count() == 1, "Вставьте ровно одну строку реакционного SMILES.");
        final String[] parts = text.strip().split("\\s+", 2);
        require(
            parts[0].length() <= 16000
                && !text.contains("|")
                && parts[0].chars().filter(c -> c == '>').count() == 2,
            "Нужен один SMILES вида исходники>агенты>продукты. CXSMILES не поддержан.");
        require(
            parts.length < 2 || !parts[1].contains("\n"),
            "Вставьте одну реакцию. Название должно занимать одну строку.");
        final IReaction rxn =
            new SmilesParser(SilentChemObjectBuilder.getInstance()).parseReactionSmiles(parts[0]);
        rxn.getReactants()
            .atomContainers()
            .forEach(g -> graphs.add(new Graph(g, ReactionComponentRole.REACTANT)));
        rxn.getAgents()
            .atomContainers()
            .forEach(g -> graphs.add(new Graph(g, ReactionComponentRole.OTHER)));
        rxn.getProducts()
            .atomContainers()
            .forEach(g -> graphs.add(new Graph(g, ReactionComponentRole.PRODUCT)));
        if (parts.length > 1 && !parts[1].isBlank()) {
          title = parts[1].strip();
        }
      } else {
        final List<String> lines = text.lines().toList();
        require(lines.size() >= 5, "Неполный заголовок RXN.");
        require(
            lines.get(0).equals(format == Format.RXN_V3000 ? "$RXN V3000" : "$RXN"),
            "Версия RXN не совпадает с выбранным форматом.");
        if (!lines.get(1).isBlank()) {
          title = lines.get(1).strip();
        }
        if (format == Format.RXN_V3000) {
          read3000(lines, graphs);
        } else {
          read2000(lines, graphs);
        }
      }
      require(
          graphs.size() <= 100
              && graphs.stream().anyMatch(g -> g.role == ReactionComponentRole.REACTANT)
              && graphs.stream().anyMatch(g -> g.role == ReactionComponentRole.PRODUCT),
          "Нужны исходник и продукт, суммарно не более 100 участников.");
      final List<Participant> participants = new ArrayList<>();
      final Set<String> notes = new LinkedHashSet<>();
      int total = 0;
      final Map<ReactionComponentRole, Integer> ordinals =
          new EnumMap<>(ReactionComponentRole.class);
      for (int i = 0; i < graphs.size(); i++) {
        cancelled();
        final Graph g = graphs.get(i);
        final int ordinal = ordinals.merge(g.role, 1, Integer::sum);
        final Map<AtomId, Integer> mapping = new LinkedHashMap<>();
        for (int j = 0; j < g.molecule.getAtomCount(); j++) {
          final int n = g.molecule.getAtom(j).getMapIdx();
          require(n >= 0 && n <= 999999, "Недопустимый номер атомной карты.");
          if (n > 0) {
            mapping.put(AtomId.of("a" + j), n);
          }
        }
        if (g.molecule.getTitle() == null || g.molecule.getTitle().isBlank()) {
          g.molecule.setTitle(
              (g.role == ReactionComponentRole.REACTANT
                      ? "Исходник "
                      : g.role == ReactionComponentRole.PRODUCT ? "Продукт " : "Агент ")
                  + ordinal);
        }
        final var structure =
            CdkStructureExchange.fromGraph(
                g.molecule, Map.of(), origin == null ? "Вставленный текст" : origin, i + 1, true);
        total += g.molecule.getAtomCount();
        require(total <= 10000, "Предел реакции — 10000 атомов.");
        participants.add(new Participant(structure, g.role, mapping));
        notes.addAll(structure.notes());
      }
      notes.add("Импортируется схема: массы, объёмы, коэффициенты и выходы нужно задать в опыте.");
      if (graphs.stream().anyMatch(g -> g.role == ReactionComponentRole.OTHER)) {
        notes.add(
            "Роль агентов не задана форматом. Уточните растворитель, катализатор или добавку перед импортом.");
      }
      if (format == Format.SMILES) {
        notes.add(
            "Раздельные фрагменты SMILES становятся отдельными участниками; для солей и многокомпонентных веществ предпочтителен RXN.");
      }
      final Entry entry = new Entry(title, participants, List.copyOf(notes));
      final LaboratoryEditor validation = new LaboratoryEditor();
      validation.importReaction(entry);
      return entry;
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          e.getMessage() == null ? "Не удалось прочитать реакцию." : e.getMessage(), e);
    }
  }

  private static IAtomContainer mol(final String text) throws Exception {
    require(text.length() <= 262144, "Компонент RXN больше 256 КБ.");
    CdkStructureExchange.preflight(text);
    try (final ISimpleChemObjectReader reader =
        text.contains("V3000")
            ? new MDLV3000Reader(new StringReader(text), IChemObjectReader.Mode.STRICT)
            : new MDLV2000Reader(new StringReader(text), IChemObjectReader.Mode.STRICT)) {
      return reader.read(new AtomContainer());
    }
  }

  private static int count(final String s) {
    final int n = Integer.parseInt(s.strip());
    require(n >= 0 && n <= 100, "Количество участников должно быть от 0 до 100.");
    return n;
  }

  private static void read2000(final List<String> lines, final List<Graph> graphs)
      throws Exception {
    final String counts = lines.get(4);
    require(
        counts.length() >= 6 && counts.length() <= 9, "Неверная строка количества участников RXN.");
    final int r = count(counts.substring(0, 3)),
        p = count(counts.substring(3, 6)),
        a = counts.length() > 6 ? count(counts.substring(6)) : 0;
    require(r + p + a <= 100, "Не более 100 участников.");
    int index = 5;
    for (int k = 0; k < r + p + a; k++) {
      require(index < lines.size() && lines.get(index++).equals("$MOL"), "Ожидался блок $MOL.");
      final StringBuilder block = new StringBuilder();
      boolean ended = false;
      while (index < lines.size()) {
        final String line = lines.get(index++);
        block.append(line).append('\n');
        if (line.stripTrailing().equals("M  END")) {
          ended = true;
          break;
        }
      }
      require(ended, "Незавершённый MOL в RXN.");
      graphs.add(
          new Graph(
              mol(block.toString()),
              k < r
                  ? ReactionComponentRole.REACTANT
                  : k < r + p ? ReactionComponentRole.PRODUCT : ReactionComponentRole.OTHER));
    }
    require(
        lines.subList(index, lines.size()).stream().allMatch(String::isBlank),
        "Лишние блоки RXN или неподдержанные свойства после реакции.");
  }

  private static void read3000(final List<String> lines, final List<Graph> graphs)
      throws Exception {
    require(lines.get(4).startsWith("M  V30 COUNTS "), "Нет строки COUNTS V3000.");
    final String[] counts = lines.get(4).substring(14).strip().split("\\s+");
    require(counts.length == 2 || counts.length == 3, "Неверные счётчики RXN V3000.");
    final int[] sizes = {
      count(counts[0]), count(counts[1]), counts.length == 3 ? count(counts[2]) : 0
    };
    require(Arrays.stream(sizes).sum() <= 100, "Не более 100 участников.");
    final String[] sides = {"REACTANT", "PRODUCT", "AGENT"};
    final ReactionComponentRole[] roles = {
      ReactionComponentRole.REACTANT, ReactionComponentRole.PRODUCT, ReactionComponentRole.OTHER
    };
    int index = 5;
    for (int side = 0; side < 3; side++) {
      if (sizes[side] == 0) {
        continue;
      }
      require(
          index < lines.size() && lines.get(index++).equals("M  V30 BEGIN " + sides[side]),
          "Нет блока " + sides[side] + ".");
      for (int k = 0; k < sizes[side]; k++) {
        require(
            index < lines.size() && lines.get(index).equals("M  V30 BEGIN CTAB"),
            "Нет CTAB участника.");
        final StringBuilder block =
            new StringBuilder("\n  Quantum  2D\n\n  0  0  0     0  0            999 V3000\n");
        boolean ended = false;
        while (index < lines.size()) {
          final String line = lines.get(index++);
          block.append(line).append('\n');
          if (line.equals("M  V30 END CTAB")) {
            ended = true;
            break;
          }
        }
        require(ended, "Незавершённый CTAB.");
        block.append("M  END\n");
        graphs.add(new Graph(mol(block.toString()), roles[side]));
      }
      require(
          index < lines.size() && lines.get(index++).equals("M  V30 END " + sides[side]),
          "Неверное завершение " + sides[side] + ".");
    }
    require(
        index < lines.size()
            && lines.get(index++).equals("M  END")
            && lines.subList(index, lines.size()).stream().allMatch(String::isBlank),
        "Неверное завершение RXN или лишние данные.");
  }

  @Override
  public Export write(
      final LaboratoryDrawing drawing,
      final String stageId,
      final Format format,
      final boolean includeMapping) {
    require(format != Format.AUTO, "Выберите формат экспорта.");
    final var stage =
        drawing.experiment().stages().stream()
            .filter(s -> s.id().equals(stageId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Выберите стадию."));
    if (includeMapping) {
      final var problems = stage.atomMapping().problems(drawing.experiment(), stage);
      require(
          problems.isEmpty(),
          String.join(" ", problems) + " Для экспорта схемы отключите атомную карту.");
    }
    final List<String> warnings = new ArrayList<>();
    warnings.add(
        "Файл содержит схему реакции. Количества, выходы, анализы и журнал сохраняются только в QCHEM.");
    warnings.add(
        "Типы агентов, текстовые условия и направление равновесия не передаются RXN/SMILES.");
    if (!includeMapping && !stage.atomMapping().empty()) {
      warnings.add("Атомная карта исключена по вашему выбору.");
    }
    if (stage.participants().stream()
        .anyMatch(
            p ->
                p.coefficient() == null
                    && (p.role() == ReactionComponentRole.REACTANT
                        || p.role() == ReactionComponentRole.PRODUCT))) {
      warnings.add(
          "Коэффициенты не заданы: каждый участник записан один раз; это не стехиометрия загрузки.");
    }
    if (stage.participants().stream()
        .anyMatch(p -> p.coefficient() != null && p.coefficient().value() > 1)) {
      warnings.add("Целые коэффициенты переданы повторными экземплярами структур.");
    }
    final List<Graph> graphs = new ArrayList<>();
    int total = 0;
    try {
      for (final var participant : stage.participants()) {
        cancelled();
        final int copies =
            participant.coefficient() == null ? 1 : participant.coefficient().value();
        require(
            graphs.size() + copies <= 100,
            "Экспорт содержит больше 100 экземпляров. Уменьшите коэффициенты.");
        final var layout = drawing.layouts().get(participant.componentId());
        require(layout != null, "Отсутствует рисунок участника.");
        final var labels =
            includeMapping
                ? stage.atomMapping().labels().stream()
                    .filter(l -> l.componentId().equals(participant.componentId()))
                    .toList()
                : List.<LaboratoryAtomMapping.Label>of();
        require(
            copies == 1 || labels.isEmpty(),
            "Нельзя повторить одну атомную карту для коэффициента > 1. Отключите карту либо задайте отдельные участники.");
        for (int k = 0; k < copies; k++) {
          final IAtomContainer graph = CdkStructureExchange.project(layout);
          graph.setTitle(layout.molecule().displayName());
          for (final var label : labels) {
            final int atom =
                java.util.stream.IntStream.range(0, layout.molecule().atoms().size())
                    .filter(i -> layout.molecule().atoms().get(i).id().equals(label.atomId()))
                    .findFirst()
                    .orElseThrow();
            graph.getAtom(atom).setMapIdx(label.number());
          }
          // Keep explicit hydrogen atoms: mapped hydrogens and ordered stereo must survive.
          total += graph.getAtomCount();
          require(total <= 10000, "Предел экспорта — 10000 атомов.");
          graphs.add(new Graph(graph, participant.role()));
        }
      }
      final var reactants =
          graphs.stream().filter(g -> g.role == ReactionComponentRole.REACTANT).toList();
      final var products =
          graphs.stream().filter(g -> g.role == ReactionComponentRole.PRODUCT).toList();
      final var agents =
          graphs.stream()
              .filter(
                  g ->
                      g.role != ReactionComponentRole.REACTANT
                          && g.role != ReactionComponentRole.PRODUCT)
              .toList();
      require(
          !reactants.isEmpty() && !products.isEmpty(),
          "Для обмена реакцией нужны исходник и продукт.");
      require(
          !stage.title().contains("\n") && !stage.title().contains("\r"),
          "Название стадии должно занимать одну строку.");
      String title = stage.title();
      final StringBuilder output = new StringBuilder();
      if (format == Format.SMILES) {
        final IReaction rxn = new Reaction();
        reactants.forEach(g -> rxn.addReactant(g.molecule));
        agents.forEach(g -> rxn.addAgent(g.molecule));
        products.forEach(g -> rxn.addProduct(g.molecule));
        output
            .append(
                new SmilesGenerator(
                        SmiFlavor.Isomeric | (includeMapping ? SmiFlavor.AtomAtomMap : 0))
                    .create(rxn))
            .append('\t')
            .append(title)
            .append('\n');
        require(
            output.length() <= 16000, "Реакционный SMILES длиннее 16000 символов; выберите RXN.");
        warnings.add(
            "SMILES не хранит отдельные имена веществ и группировку раздельных фрагментов. Имена стадии и структуры сохраняйте также в QCHEM.");
      } else {
        if (title.length() > 80) {
          title = title.substring(0, 80);
          warnings.add("Название в заголовке RXN сокращено до 80 символов.");
        }
        output
            .append(format == Format.RXN_V3000 ? "$RXN V3000\n" : "$RXN\n")
            .append(title)
            .append("\nQuantum IR Studio\n\n");
        if (format == Format.RXN_V2000) {
          output.append(
              String.format(
                  Locale.ROOT, "%3d%3d%3d\n", reactants.size(), products.size(), agents.size()));
          for (final var side : List.of(reactants, products, agents)) {
            for (final var g : side) {
              output.append("$MOL\n").append(writeMol(g.molecule, false));
            }
          }
          if (!agents.isEmpty()) {
            warnings.add(
                "Агенты V2000 используют расширение RXN: проверьте поддержку в принимающей программе.");
          }
        } else {
          output
              .append("M  V30 COUNTS ")
              .append(reactants.size())
              .append(' ')
              .append(products.size())
              .append(' ')
              .append(agents.size())
              .append('\n');
          final String[] names = {"REACTANT", "PRODUCT", "AGENT"};
          int i = 0;
          for (final var side : List.of(reactants, products, agents)) {
            final String name = names[i++];
            if (side.isEmpty()) {
              continue;
            }
            output.append("M  V30 BEGIN ").append(name).append('\n');
            for (final var g : side) {
              final String mol = writeMol(g.molecule, true);
              output.append(mol, mol.indexOf("M  V30 BEGIN CTAB"), mol.indexOf("M  END"));
            }
            output.append("M  V30 END ").append(name).append('\n');
          }
          output.append("M  END\n");
          warnings.add(
              "RXN V3000 не хранит имена отдельных веществ; агенты поддерживаются не всеми программами.");
        }
      }
      require(
          output.length() <= CdkStructureExchange.MAX_TEXT, "Экспорт превышает 4 млн символов.");
      return new Export(output.toString(), warnings);
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          e.getMessage() == null ? "Не удалось записать реакцию." : e.getMessage(), e);
    }
  }

  private static String writeMol(final IAtomContainer graph, final boolean v3000) throws Exception {
    if (!v3000) {
      require(
          graph.getAtomCount() <= 999 && graph.getBondCount() <= 999,
          "Для более 999 атомов или связей выберите V3000.");
      for (final IAtom a : graph.atoms()) {
        require(a.getMapIdx() <= 999, "Номера атомной карты > 999 требуют V3000.");
      }
    }
    new StructureDiagramGenerator().generateCoordinates(graph);
    final StringWriter out = new StringWriter();
    try (final IChemObjectWriter writer =
        v3000 ? new MDLV3000Writer(out) : new MDLV2000Writer(out)) {
      writer.write(graph);
    }
    return out.toString().replace("\r\n", "\n");
  }
}
