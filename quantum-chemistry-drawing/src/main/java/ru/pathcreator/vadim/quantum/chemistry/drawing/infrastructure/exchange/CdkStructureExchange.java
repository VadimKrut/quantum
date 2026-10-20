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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.io.*;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import org.openscience.cdk.smiles.*;
import org.openscience.cdk.stereo.*;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.StructureExchange;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.geometry.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Bounded, conservative exchange; ordered tetrahedral parity is retained, never guessed as CIP. */
public final class CdkStructureExchange implements StructureExchange {
  public static final int MAX_TEXT = 4 * 1024 * 1024;
  private static final String PREFIX = "exchange.v1.",
      PARITY = Stereochemistry.ORDERED_PARITY_ATTRIBUTE,
      SIGNATURE = PREFIX + "signature",
      FIELD = PREFIX + "field.";

  private static void require(final boolean condition, final String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  private static void cancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
  }

  @Override
  public List<Entry> read(final String input, final Format requested, final String source) {
    require(
        input != null && !input.isBlank() && input.length() <= MAX_TEXT,
        "Введите структуру; предел текста — 4 млн символов.");
    final String text = input.replace("\r\n", "\n").replace('\r', '\n').replaceFirst("^\uFEFF", "");
    final Format format =
        requested == Format.AUTO
            ? text.contains("M  END")
                ? text.contains("$$$$") ? Format.SDF : Format.MOL
                : Format.SMILES
            : requested;
    final List<String> records =
        format == Format.SMILES
            ? text.lines().filter(s -> !s.isBlank()).toList()
            : format == Format.SDF
                ? Arrays.stream(text.split("(?m)^\\$\\$\\$\\$[ \\t]*$", -1))
                    .filter(s -> !s.isBlank())
                    .toList()
                : List.of(text);
    require(records.size() <= 100, "За один импорт можно просмотреть не более 100 веществ.");
    final List<Entry> result = new ArrayList<>();
    int total = 0;
    for (int index = 0; index < records.size(); index++) {
      cancelled();
      try {
        String record = records.get(index);
        require(record.length() <= 262144, "Запись слишком большая: предел 256 КБ текста.");
        if (format == Format.SDF && index > 0 && record.startsWith("\n")) {
          record = record.substring(1);
        }
        final Map<String, String> fields = new LinkedHashMap<>();
        final IAtomContainer graph;
        if (format == Format.SMILES) {
          final String[] parts = record.strip().split("\\s+", 2);
          require(parts[0].length() <= 16000, "SMILES слишком длинный.");
          require(
              !parts[0].contains(">") && !record.contains("|"),
              "Реакционный SMILES и CXSMILES пока не поддержаны.");
          graph = parseSmiles(parts[0]);
          graph.setTitle(parts.length > 1 ? parts[1] : "Вещество " + (index + 1));
        } else {
          final Matcher end = Pattern.compile("(?m)^M  END[ \\t]*$").matcher(record);
          require(end.find(), "Нет завершающей строки M  END.");
          final String mol = record.substring(0, end.end()) + "\n",
              tail = record.substring(end.end()).strip();
          require(
              format == Format.SDF || tail.isEmpty(),
              "После MOL найдены дополнительные данные; выберите SDF.");
          preflight(mol);
          try (final ISimpleChemObjectReader reader =
              mol.contains("V3000")
                  ? new MDLV3000Reader(new StringReader(mol), IChemObjectReader.Mode.STRICT)
                  : new MDLV2000Reader(new StringReader(mol), IChemObjectReader.Mode.STRICT)) {
            graph = reader.read(new AtomContainer());
          }
          if (!tail.isEmpty()) {
            readFields(tail, fields);
          }
          if (graph.getTitle() == null || graph.getTitle().isBlank()) {
            graph.setTitle("Вещество " + (index + 1));
          }
        }
        result.add(
            fromGraph(
                graph,
                fields,
                source == null || source.isBlank() ? "Вставленный текст" : source,
                index + 1,
                false));
        total += graph.getAtomCount();
        require(total <= 10000, "Предел набора — 10000 атомов.");
      } catch (final java.util.concurrent.CancellationException e) {
        throw e;
      } catch (final Exception e) {
        throw new IllegalArgumentException(
            "Запись "
                + (index + 1)
                + ": "
                + (e.getMessage() == null ? "не удалось прочитать структуру" : e.getMessage()),
            e);
      }
    }
    return List.copyOf(result);
  }

  static Entry fromGraph(
      final IAtomContainer graph,
      final Map<String, String> fields,
      final String source,
      final int index,
      final boolean allowMapping)
      throws Exception {
    require(
        graph != null && graph.getAtomCount() > 0 && graph.getAtomCount() <= 1000,
        "Допустимы 1–1000 атомов на вещество.");
    require(
        graph.getSingleElectronCount() == 0 && graph.getLonePairCount() == 0,
        "Радикалы и явные электронные пары пока не поддержаны обменом.");
    for (final IAtom atom : graph.atoms()) {
      require(
          atom.getAtomicNumber() != null
              && atom.getAtomicNumber() > 0
              && !(atom instanceof IPseudoAtom)
              && !(atom instanceof org.openscience.cdk.isomorphism.matchers.IQueryAtom)
              && (allowMapping || atom.getMapIdx() == 0),
          "Запросы, R-группы и атомные карты пока не поддержаны.");
    }
    require(
        graph.getProperty(org.openscience.cdk.CDKConstants.CTAB_SGROUPS) == null,
        "S-группы и полимеры пока не поддержаны.");
    for (final IBond bond : graph.bonds()) {
      require(
          !(bond instanceof org.openscience.cdk.isomorphism.matchers.IQueryBond),
          "Связи запросов пока не поддержаны.");
      require(
          bond.getOrder() == IBond.Order.SINGLE
              || bond.getOrder() == IBond.Order.DOUBLE
              || bond.getOrder() == IBond.Order.TRIPLE,
          "Неопределённая или неподдержанная связь.");
      require(
          bond.getStereo() != IBond.Stereo.UP_OR_DOWN
              && bond.getStereo() != IBond.Stereo.UP_OR_DOWN_INVERTED
              && bond.getStereo() != IBond.Stereo.E_OR_Z,
          "Неопределённая стереосвязь требует ручного ввода в QCHEM.");
    }
    AtomContainerManipulator.convertImplicitToExplicitHydrogens(graph);
    closedShell(graph);
    require(graph.getAtomCount() <= 1000, "Не более 1000 атомов, включая водород.");
    return convert(graph, fields, source, index);
  }

  private static IAtomContainer parseSmiles(final String smiles) {
    try {
      return new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles);
    } catch (final org.openscience.cdk.exception.InvalidSmilesException e) {
      throw new IllegalArgumentException(
          "Не удалось прочитать SMILES. Проверьте скобки, номера замыканий и обозначения атомов.",
          e);
    }
  }

  private static void closedShell(final IAtomContainer graph)
      throws org.openscience.cdk.exception.CDKException {
    final var matcher =
        org.openscience.cdk.atomtype.CDKAtomTypeMatcher.getInstance(
            SilentChemObjectBuilder.getInstance(),
            org.openscience.cdk.atomtype.CDKAtomTypeMatcher.REQUIRE_EXPLICIT_HYDROGENS);
    for (final IAtom atom : graph.atoms()) {
      cancelled();
      final var type = matcher.findMatchingAtomType(graph, atom);
      require(
          type != null && !"X".equals(type.getAtomTypeName()),
          "Не определено закрытооболочечное состояние атома "
              + (graph.indexOf(atom) + 1)
              + ". Радикалы, карбены и нестандартная валентность пока не поддержаны обменом.");
    }
  }

  static void preflight(final String mol) {
    require(mol.contains("V2000") || mol.contains("V3000"), "Поддержаны MOL V2000 и V3000.");
    for (final String line : mol.lines().toList()) {
      if (line.startsWith("M  ") && !line.startsWith("M  V30 ")) {
        require(
            line.matches("M  (END|CHG|ISO)(?:.*)"),
            "Расширение MOL пока не поддержано: " + line.substring(0, Math.min(8, line.length())));
      }
      require(
          !line.startsWith("A  ") && !line.startsWith("V  "),
          "Псевдоатомы и дополнительные атомные подписи пока не поддержаны.");
      if (line.startsWith("M  V30 ")) {
        final Matcher properties = Pattern.compile("([A-Z][A-Z0-9_]*)=").matcher(line);
        while (properties.find()) {
          require(
              (Set.of("CHG", "MASS", "CFG", "VAL").contains(properties.group(1))
                  || properties.group(1).equals("ATOMS")
                      && line.startsWith("M  V30 MDLV30/STEABS ATOMS=")),
              "Атрибут V3000 пока не поддержан: " + properties.group(1));
        }
        require(
            !line.contains("SGROUP")
                && (!line.contains("COLLECTION")
                    || line.equals("M  V30 BEGIN COLLECTION")
                    || line.equals("M  V30 END COLLECTION"))
                && !line.contains("STEREL")
                && !line.contains("STERAC")
                && !line.contains("LINKNODE")
                && !line.contains("RGROUP"),
            "Группы, полимеры и относительная стереохимия V3000 пока не поддержаны.");
      }
    }
  }

  private static void readFields(final String tail, final Map<String, String> fields) {
    final String[] lines = tail.split("\n", -1);
    int i = 0;
    while (i < lines.length) {
      if (lines[i].isBlank()) {
        i++;
        continue;
      }
      final Matcher m =
          Pattern.compile(">[ \\t]+(?:[0-9]+[ \\t]+)?<([^<>]+)>.*").matcher(lines[i++]);
      require(m.matches(), "Неверный заголовок свойства SDF.");
      final String key = m.group(1);
      require(
          key.length() <= 120 && !fields.containsKey(key),
          "Повторное или слишком длинное имя свойства SDF.");
      final StringBuilder value = new StringBuilder();
      while (i < lines.length && !lines[i].isBlank()) {
        if (!value.isEmpty()) {
          value.append('\n');
        }
        value.append(lines[i++]);
      }
      require(
          value.length() <= 16000 && fields.size() < 100, "Слишком много данных в свойствах SDF.");
      fields.put(key, value.toString());
    }
  }

  private static Entry convert(
      final IAtomContainer graph,
      final Map<String, String> fields,
      final String source,
      final int record)
      throws Exception {
    final List<ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom> atoms =
        new ArrayList<>();
    final List<ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond> bonds =
        new ArrayList<>();
    final List<Stereocenter> centers = new ArrayList<>();
    final List<String> parity = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    for (final IAtom a : graph.atoms()) {
      atoms.add(
          ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom.of(
              id(graph, a),
              ElementSymbol.of(a.getSymbol()),
              null,
              FormalCharge.of(a.getFormalCharge() == null ? 0 : a.getFormalCharge()),
              a.getMassNumber() == null ? null : Isotope.of(a.getMassNumber()),
              RadicalState.CLOSED_SHELL,
              ChemistryMetadata.EMPTY));
    }
    for (final IBond b : graph.bonds()) {
      bonds.add(
          ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond.of(
              id(graph, b.getBegin()),
              id(graph, b.getEnd()),
              b.isAromatic()
                  ? BondType.AROMATIC
                  : b.getOrder() == IBond.Order.DOUBLE
                      ? BondType.DOUBLE
                      : b.getOrder() == IBond.Order.TRIPLE ? BondType.TRIPLE : BondType.SINGLE));
    }
    for (final IStereoElement<?, ?> stereo : graph.stereoElements()) {
      require(
          stereo.getGroupInfo() == IStereoElement.GRP_ABS,
          "Относительные и рацемические стереогруппы пока не поддержаны.");
      if (stereo instanceof ITetrahedralChirality s) {
        final var refs = Arrays.stream(s.getLigands()).map(a -> id(graph, a)).toList();
        require(
            !refs.contains(id(graph, s.getChiralAtom())),
            "Тетраэдрический центр с неявной электронной парой пока не поддержан.");
        centers.add(
            Stereocenter.ofTetrahedralAtom(
                id(graph, s.getChiralAtom()),
                StereochemicalDescriptor.UNKNOWN,
                refs.get(0),
                refs.get(1),
                refs.get(2),
                refs.get(3)));
        parity.add(
            id(graph, s.getChiralAtom()).value()
                + ":"
                + s.getStereo().name()
                + ":"
                + String.join(",", refs.stream().map(AtomId::value).toList()));
      } else if (stereo instanceof IDoubleBondStereochemistry s) {
        final var bond = s.getStereoBond();
        centers.add(
            Stereocenter.ofDoubleBond(
                id(graph, bond.getBegin()),
                id(graph, bond.getEnd()),
                s.getStereo() == IDoubleBondStereochemistry.Conformation.TOGETHER
                    ? StereochemicalDescriptor.CIS
                    : StereochemicalDescriptor.TRANS,
                id(graph, s.getBonds()[0].getOther(bond.getBegin())),
                id(graph, s.getBonds()[1].getOther(bond.getEnd()))));
      } else {
        throw new IllegalArgumentException(
            "Осевая и координационная стереохимия пока не поддержана обменом.");
      }
    }
    final Map<String, String> attrs = new LinkedHashMap<>();
    attrs.put(PREFIX + "source", source);
    attrs.put(PREFIX + "record", Integer.toString(record));
    fields.forEach(
        (k, v) ->
            attrs.put(
                FIELD
                    + Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(k.getBytes(StandardCharsets.UTF_8)),
                Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8)) + "."));
    if (!parity.isEmpty()) {
      attrs.put(PARITY, String.join(";", parity));
      notes.add(
          "Тетраэдрическая конфигурация сохранена. R/S автоматически не назначается; в таблице стерео показано «не задано».");
    }
    if (centers.stream().anyMatch(c -> c.kind() == StereocenterKind.DOUBLE_BOND)) {
      notes.add(
          "Для двойных связей сохранено взаимное расположение выбранных заместителей; это не автоматическое назначение E/Z.");
    }
    Molecule molecule =
        Molecule.of(
            MoleculeId.of("import_" + UUID.randomUUID()),
            graph.getTitle(),
            atoms,
            bonds,
            MolecularCharge.of(atoms.stream().mapToInt(a -> a.formalCharge().value()).sum()),
            SpinMultiplicity.SINGLET,
            Stereochemistry.of(centers),
            ChemistryMetadata.of(null, null, attrs));
    if (!parity.isEmpty()) {
      attrs.put(SIGNATURE, MolecularIdentity.signature(molecule));
      molecule = metadata(molecule, attrs);
    }
    new StructureDiagramGenerator().generateCoordinates(graph);
    cancelled();
    final List<AtomDrawing> positions = new ArrayList<>();
    final List<BondDrawing> styles = new ArrayList<>();
    for (final IAtom a : graph.atoms()) {
      final var p = a.getPoint2d();
      require(
          p != null && Double.isFinite(p.x) && Double.isFinite(p.y),
          "Не удалось построить 2D-раскладку.");
      final var q = a.getPoint3d();
      positions.add(
          AtomDrawing.of(
              id(graph, a),
              DrawingPoint2D.of(p.x * 28, -p.y * 28),
              q == null ? DrawingPoint3D.ORIGIN : DrawingPoint3D.of(q.x, q.y, q.z),
              !a.getSymbol().equals("C")));
    }
    for (final IBond b : graph.bonds()) {
      final var s = b.getStereo();
      final boolean reverse = s == IBond.Stereo.UP_INVERTED || s == IBond.Stereo.DOWN_INVERTED;
      styles.add(
          BondDrawing.of(
              id(graph, reverse ? b.getEnd() : b.getBegin()),
              id(graph, reverse ? b.getBegin() : b.getEnd()),
              s == IBond.Stereo.UP || s == IBond.Stereo.UP_INVERTED
                  ? BondDrawingStyle.WEDGE_UP
                  : s == IBond.Stereo.DOWN || s == IBond.Stereo.DOWN_INVERTED
                      ? BondDrawingStyle.WEDGE_DOWN
                      : BondDrawingStyle.PLAIN));
    }
    if (!fields.isEmpty()) {
      notes.add("Исходные поля SDF сохранены отдельно. Количества и выходы из них не назначены.");
    }
    notes.add("2D-рисунок построен заново. Экспорт MOL/SDF создаёт 2D-структуры.");
    return new Entry(MoleculeDrawing.of(molecule, positions, styles, List.of()), fields, notes);
  }

  private static AtomId id(final IAtomContainer g, final IAtom a) {
    return AtomId.of("a" + g.indexOf(a));
  }

  private static Molecule metadata(final Molecule m, final Map<String, String> attrs) {
    return Molecule.of(
        m.id(),
        m.displayName(),
        m.atoms(),
        m.bonds(),
        m.charge(),
        m.spinMultiplicity(),
        m.stereochemistry(),
        m.conformation(),
        m.opticalRotation(),
        m.symmetry(),
        ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), attrs));
  }

  public static Map<String, String> fields(final Molecule molecule) {
    final Map<String, String> fields = new TreeMap<>();
    molecule
        .metadata()
        .attributes()
        .forEach(
            (k, v) -> {
              if (k.startsWith(FIELD)) {
                require(v.endsWith("."), "Повреждено свойство SDF.");
                fields.put(
                    new String(
                        Base64.getUrlDecoder().decode(k.substring(FIELD.length())),
                        StandardCharsets.UTF_8),
                    new String(
                        Base64.getDecoder().decode(v.substring(0, v.length() - 1)),
                        StandardCharsets.UTF_8));
              }
            });
    return Map.copyOf(fields);
  }

  @Override
  public String write(final List<MoleculeDrawing> structures, final Format format) {
    require(!structures.isEmpty() && structures.size() <= 100, "Выберите от 1 до 100 веществ.");
    require(format != Format.AUTO, "Выберите формат экспорта.");
    require(
        format != Format.MOL || structures.size() == 1,
        "MOL содержит одно вещество; для набора выберите SDF или SMILES.");
    final StringBuilder result = new StringBuilder();
    for (int index = 0; index < structures.size(); index++) {
      cancelled();
      try {
        final var drawing = structures.get(index);
        final IAtomContainer graph = project(drawing);
        graph.setTitle(drawing.molecule().displayName());
        require(
            !graph.getTitle().contains("\n") && !graph.getTitle().contains("\r"),
            "Название должно занимать одну строку.");
        if (format == Format.SMILES) {
          result
              .append(
                  new SmilesGenerator(SmiFlavor.Isomeric)
                      .create(AtomContainerManipulator.suppressHydrogens(graph)))
              .append('\t')
              .append(graph.getTitle())
              .append('\n');
        } else {
          final var compact = AtomContainerManipulator.suppressHydrogens(graph);
          new StructureDiagramGenerator().generateCoordinates(compact);
          final StringWriter text = new StringWriter();
          try (final MDLV2000Writer writer = new MDLV2000Writer(text)) {
            writer.write(compact);
          }
          result.append(text);
          if (format == Format.SDF) {
            for (final var e : new TreeMap<>(fields(drawing.molecule())).entrySet()) {
              require(!e.getKey().matches(".*[<>\\r\\n].*"), "Неподдержанное имя свойства SDF.");
              result
                  .append(">  <")
                  .append(e.getKey())
                  .append(">\n")
                  .append(e.getValue())
                  .append("\n\n");
            }
            result.append("$$$$\n");
          }
        }
        require(result.length() <= MAX_TEXT, "Экспорт превышает предел 4 млн символов.");
      } catch (final Exception e) {
        throw new IllegalArgumentException("Вещество " + (index + 1) + ": " + e.getMessage(), e);
      }
    }
    return result.toString();
  }

  static IAtomContainer project(final MoleculeDrawing drawing) throws Exception {
    final Molecule m = drawing.molecule();
    require(
        !MolecularRelativeStereo.present(m),
        "Относительные / рацемические группы нельзя без потерь записать в выбранный обменный формат. Сохраните QCHEM; группы не будут отброшены автоматически.");
    require(m.atomCount() > 0 && m.atomCount() <= 999, "Предел экспорта — 999 атомов.");
    require(
        m.spinMultiplicity().value() == 1
            && m.atoms().stream().noneMatch(a -> a.radicalState().radical()),
        "Радикалы и спин пока не поддержаны экспортом.");
    require(
        m.charge().value() == m.atoms().stream().mapToInt(a -> a.formalCharge().value()).sum(),
        "Уточните заряды атомов: их сумма отличается от заряда молекулы.");
    require(
        !m.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE),
        "Сначала перепроверьте стереохимию после изменения структуры.");
    final IAtomContainer graph = new AtomContainer();
    final Map<AtomId, IAtom> atoms = new HashMap<>();
    for (final var a : m.atoms()) {
      final IAtom copy = new org.openscience.cdk.silent.Atom(a.element().symbol().value());
      copy.setFormalCharge(a.formalCharge().value());
      copy.setImplicitHydrogenCount(0);
      if (a.hasIsotope()) {
        copy.setMassNumber(a.isotope().massNumber());
      }
      graph.addAtom(copy);
      atoms.put(a.id(), copy);
    }
    for (final var b : m.bonds()) {
      final IBond.Order order =
          switch (b.type()) {
            case SINGLE, AROMATIC -> IBond.Order.SINGLE;
            case DOUBLE -> IBond.Order.DOUBLE;
            case TRIPLE -> IBond.Order.TRIPLE;
            default ->
                throw new IllegalArgumentException(
                    "Координационные и неизвестные связи пока не поддержаны.");
          };
      final IBond bond =
          new org.openscience.cdk.silent.Bond(
              atoms.get(b.firstAtomId()), atoms.get(b.secondAtomId()), order);
      if (b.type() == BondType.AROMATIC) {
        bond.setIsAromatic(true);
        bond.getBegin().setIsAromatic(true);
        bond.getEnd().setIsAromatic(true);
      }
      graph.addBond(bond);
    }
    if (m.bonds().stream().anyMatch(b -> b.type() == BondType.AROMATIC)) {
      org.openscience.cdk.aromaticity.Kekulization.kekulize(graph);
    }
    final Map<AtomId, String[]> parity = new HashMap<>();
    final String raw = m.metadata().attributes().get(PARITY);
    if (raw != null) {
      require(
          MolecularIdentity.signature(m).equals(m.metadata().attributes().get(SIGNATURE)),
          "Структура изменилась после импорта. Сохранённая стереоконфигурация требует нового ввода; экспорт в QCHEM доступен.");
      for (final String item : raw.split(";")) {
        final String[] parts = item.split(":");
        require(parts.length == 3, "Повреждена импортированная конфигурация.");
        parity.put(AtomId.of(parts[0]), parts);
      }
    }
    for (final var s : m.stereochemistry().centers()) {
      if (s.kind() == StereocenterKind.TETRAHEDRAL_ATOM
          && s.descriptor() == StereochemicalDescriptor.UNKNOWN
          && parity.containsKey(s.primaryAtomId())) {
        final var p = parity.remove(s.primaryAtomId());
        final var refs = Arrays.stream(p[2].split(",")).map(AtomId::of).toList();
        require(refs.equals(s.referenceAtomIds()), "Изменился порядок стереозаместителей.");
        graph.addStereoElement(
            new TetrahedralChirality(
                atoms.get(s.primaryAtomId()),
                refs.stream().map(atoms::get).toArray(IAtom[]::new),
                ITetrahedralChirality.Stereo.valueOf(p[1])));
      } else if (s.kind() == StereocenterKind.DOUBLE_BOND
          && (s.descriptor() == StereochemicalDescriptor.CIS
              || s.descriptor() == StereochemicalDescriptor.TRANS)) {
        final var a = atoms.get(s.primaryAtomId());
        final var b = atoms.get(s.secondaryAtomId());
        graph.addStereoElement(
            new DoubleBondStereochemistry(
                graph.getBond(a, b),
                new IBond[] {
                  graph.getBond(a, atoms.get(s.referenceAtomIds().get(0))),
                  graph.getBond(b, atoms.get(s.referenceAtomIds().get(1)))
                },
                s.descriptor() == StereochemicalDescriptor.CIS
                    ? IDoubleBondStereochemistry.Conformation.TOGETHER
                    : IDoubleBondStereochemistry.Conformation.OPPOSITE));
      } else {
        throw new IllegalArgumentException(
            "Для этой стереозаписи экспорт без потерь пока недоступен. Сохраните QCHEM; R/S и E/Z не преобразуются в геометрию автоматически.");
      }
    }
    closedShell(graph);
    require(
        parity.isEmpty(), "Импортированная конфигурация больше не соответствует стереозаписям.");
    require(
        !m.stereochemistry().empty()
            || drawing.bonds().stream()
                .noneMatch(
                    b ->
                        b.style() == BondDrawingStyle.WEDGE_UP
                            || b.style() == BondDrawingStyle.WEDGE_DOWN
                            || b.style() == BondDrawingStyle.WAVY
                            || b.style() == BondDrawingStyle.UNKNOWN),
        "Клинья без стереозаписи нельзя безопасно выгрузить; уточните конфигурацию.");
    return graph;
  }
}
