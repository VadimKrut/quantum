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
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.vecmath.Point2d;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.silent.*;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Bounded CDXML chemical interchange. Page graphics remain in the preserved source. */
public final class ChemDrawXmlExchange implements ChemDrawExchange {
  public static final int MAX_BYTES = 8_000_000;
  private static final Set<String> VISUAL =
      Set.of(
          "id",
          "Z",
          "p",
          "BoundingBox",
          "color",
          "bgcolor",
          "Visible",
          "LabelDisplay",
          "LabelJustification",
          "LabelAlignment",
          "ShowAtomQuery",
          "ShowAtomStereo",
          "ShowAtomNumber",
          "ShowAtomEnhancedStereo",
          "ShowTerminalCarbonLabels",
          "ShowNonTerminalCarbonLabels",
          "HideImplicitHydrogens",
          "ShowBondQuery",
          "ShowBondStereo",
          "ShowBondRxn",
          "DoublePosition",
          "CrossingBonds",
          "IgnoreWarnings",
          "Warning",
          "xyz",
          "NeedsClean",
          "MarginWidth",
          "LineWidth",
          "BoldWidth",
          "HashSpacing");

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

  private static List<Element> children(final Element e) {
    final List<Element> result = new ArrayList<>();
    for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element v) {
        result.add(v);
      }
    }
    return result;
  }

  private static List<Element> all(final Element e, final String tag) {
    final List<Element> result = new ArrayList<>();
    final var list = e.getElementsByTagName(tag);
    for (int i = 0; i < list.getLength(); i++) {
      result.add((Element) list.item(i));
    }
    return result;
  }

  private static String attr(final Element e, final String name, final String fallback) {
    return e.hasAttribute(name) ? e.getAttribute(name) : fallback;
  }

  private static int integer(final String value) {
    try {
      return new java.math.BigDecimal(value).intValueExact();
    } catch (final RuntimeException ex) {
      throw new IllegalArgumentException("Ожидалось целое число: " + value);
    }
  }

  private static String id(final Element e) {
    final String id = e.getAttribute("id");
    require(
        id.matches("[1-9][0-9]{0,9}") && Long.parseLong(id) <= 4294967295L,
        "Объект без допустимого уникального id.");
    return id;
  }

  private static List<String> refs(final Element e, final String attribute) {
    final String text = e.getAttribute(attribute).strip();
    return text.isEmpty() ? List.of() : List.of(text.split("\\s+"));
  }

  private static boolean yes(final Element e, final String name) {
    return Set.of("yes", "true", "1").contains(e.getAttribute(name).toLowerCase(Locale.ROOT));
  }

  private static void attributes(final Element e, final Set<String> accepted) {
    for (int i = 0; i < e.getAttributes().getLength(); i++) {
      final String name = e.getAttributes().item(i).getNodeName();
      require(
          VISUAL.contains(name) || accepted.contains(name),
          "Свойство «" + name + "» требует отдельного разбора; структура не перенесена.");
    }
  }

  private static String caption(final Element e) {
    return all(e, "s").stream().map(Element::getTextContent).reduce("", String::concat).strip();
  }

  @Override
  public ChemDrawExchange.Document read(final byte[] bytes, final String source) {
    require(bytes.length > 0 && bytes.length <= MAX_BYTES, "Допустим файл CDXML до 8 МБ.");
    require(
        !(bytes.length >= 8
            && new String(bytes, 0, 8, java.nio.charset.StandardCharsets.US_ASCII)
                .equals("VjCD0100")),
        "Это двоичный CDX. Сохраните в ChemDraw копию CDXML или используйте Copy as CDXML.");
    try {
      final var factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", "64");
      factory.setExpandEntityReferences(false);
      factory.setXIncludeAware(false);
      final var builder = factory.newDocumentBuilder();
      builder.setEntityResolver(
          (publicId, systemId) -> new org.xml.sax.InputSource(new StringReader("")));
      builder.setErrorHandler(
          new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
              throw e;
            }

            @Override
            public void fatalError(org.xml.sax.SAXParseException e)
                throws org.xml.sax.SAXException {
              throw e;
            }
          });
      final var xml = builder.parse(new ByteArrayInputStream(bytes));
      require(
          xml.getDoctype() == null || xml.getDoctype().getInternalSubset() == null,
          "Внутренние объявления DTD и сущностей не поддержаны.");
      final Element root = xml.getDocumentElement();
      require(root.getTagName().equals("CDXML"), "Ожидался документ CDXML.");
      final var every = all(root, "*");
      require(every.size() <= 50000, "В документе больше 50000 объектов.");
      final Map<String, Element> objects = new LinkedHashMap<>();
      for (final var e : every) {
        cancelled();
        if (e.hasAttribute("id") && !e.getTagName().equals("font")) {
          require(objects.put(id(e), e) == null, "Повторный id " + id(e));
        }
      }
      final var notes = new ArrayList<String>();
      notes.add(
          "Конфигурация читается по связям и координатам; подписи AS/BS (R/S, E/Z) из исходника не считаются независимым расчётом CIP.");
      notes.add(
          "Переносятся химические структуры и явно описанные стадии. Сокращения с одной точкой связи разворачиваются только по вложенному графу исходника. 2D-раскладка строится заново. Оформление страниц, рисунки и исходные подписи остаются в приложенном CDXML.");
      final List<Fragment> fragments = new ArrayList<>();
      final Map<String, Fragment> byId = new LinkedHashMap<>();
      int total = 0;
      for (final var f : all(root, "fragment")) {
        cancelled();
        if (hasAncestor(f, "n")) {
          continue;
        }
        require(fragments.size() < 100, "Не более 100 фрагментов в документе.");
        final String key = id(f);
        final String title = attr(f, "Name", "Вещество " + (fragments.size() + 1));
        Fragment result;
        try {
          require(
              all(root, "bracketedgroup").isEmpty() && all(root, "altgroup").isEmpty(),
              "Полимерные скобки, группы повторения или альтернативные группы требуют отдельного разбора. Исходник сохранён.");
          result = fragment(f, title, source);
          total += result.entry().drawing().molecule().atomCount();
          require(total <= 10000, "Не более 10000 атомов во всём документе.");
        } catch (final java.util.concurrent.CancellationException e) {
          throw e;
        } catch (final Exception e) {
          result = new Fragment(key, title, null, Map.of(), message(e));
        }
        fragments.add(result);
        byId.put(key, result);
      }
      require(!fragments.isEmpty(), "В документе не найдено молекулярных фрагментов.");
      require(total <= 10000, "Не более 10000 атомов во всём документе.");
      final List<Step> steps = new ArrayList<>();
      for (final var e : all(root, "step")) {
        cancelled();
        require(steps.size() < 100, "Не более 100 стадий в документе.");
        final String key = id(e), title = attr(e, "Name", "Стадия " + (steps.size() + 1));
        try {
          steps.add(step(e, title, objects, byId));
        } catch (final Exception failure) {
          steps.add(
              new Step(
                  key,
                  title,
                  List.of(),
                  LaboratoryReactionStage.Arrow.FORWARD,
                  "",
                  message(failure)));
        }
      }
      if (steps.isEmpty()) {
        notes.add(
            "В исходнике нет явных объектов стадий. Роли и направление реакции не определяются по расположению рисунков.");
      }
      if (every.stream()
          .anyMatch(
              e ->
                  !Set.of(
                          "page",
                          "group",
                          "fragment",
                          "n",
                          "b",
                          "t",
                          "s",
                          "scheme",
                          "step",
                          "graphic",
                          "arrow",
                          "fonttable",
                          "font",
                          "colortable",
                          "color")
                      .contains(e.getTagName()))) {
        notes.add(
            "В документе есть дополнительные объекты: они сохранены в исходнике и не превращены в редактируемую химию.");
      }
      for (final var f : fragments) {
        if (!f.supported()) {
          notes.add(f.title() + ": " + f.problem());
        }
      }
      return new ChemDrawExchange.Document(fragments, steps, notes);
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException("CDXML: " + message(e), e);
    }
  }

  private static boolean hasAncestor(final Element e, final String tag) {
    for (Node p = e.getParentNode(); p instanceof Element; p = p.getParentNode()) {
      if (((Element) p).getTagName().equals(tag)) {
        return true;
      }
    }
    return false;
  }

  private static String message(final Exception e) {
    return e.getMessage() == null ? "Не удалось прочитать объект." : e.getMessage();
  }

  private static Fragment fragment(final Element original, final String title, final String source)
      throws Exception {
    final Element f = (Element) original.cloneNode(true);
    expandNicknames(f, 0);
    attributes(
        f,
        Set.of(
            "Name",
            "Comment",
            "Racemic",
            "Relative",
            "Absolute",
            "Formula",
            "Weight",
            "Charge",
            "SpinMultiplicity",
            "ConnectionOrder"));
    require(
        !yes(f, "Racemic") && !yes(f, "Relative"),
        "Рацемическая и относительная стереохимия пока не поддержаны.");
    require(
        !f.hasAttribute("ConnectionOrder"),
        "Фрагменты с внешними точками присоединения пока не поддержаны.");
    final IAtomContainer graph = new AtomContainer();
    graph.setTitle(title);
    final Map<String, IAtom> atoms = new LinkedHashMap<>();
    final Map<String, AtomId> atomIds = new LinkedHashMap<>();
    final Map<Integer, Integer> hydrogens = new HashMap<>();
    boolean wedge = false;
    for (final var n : children(f)) {
      require(
          Set.of("n", "b", "t", "objecttag", "annotation").contains(n.getTagName()),
          "Вложенные графические/химические группы требуют отдельного разбора.");
      if (!n.getTagName().equals("n")) {
        continue;
      }
      require(graph.getAtomCount() < 1000, "Не более 1000 атомов в веществе.");
      require(
          attr(n, "NodeType", "Element").equals("Element"),
          "Разверните сокращённые группы и R-группы в исходном редакторе (NodeType="
              + n.getAttribute("NodeType")
              + ").");
      attributes(
          n,
          Set.of(
              "NodeType",
              "Element",
              "Charge",
              "Isotope",
              "NumHydrogens",
              "AS",
              "EnhancedStereoType",
              "EnhancedStereoGroupNum",
              "AtomNumber",
              "Geometry",
              "BondOrdering"));
      require(all(n, "fragment").isEmpty(), "Вложенная сокращённая группа пока не поддержана.");
      require(
          !n.hasAttribute("EnhancedStereoType")
              || Set.of("Unspecified", "Absolute").contains(n.getAttribute("EnhancedStereoType")),
          "Относительная или рацемическая стереогруппа пока не поддержана.");
      require(!n.hasAttribute("EnhancedStereoGroupNum"), "Стереогруппы пока не поддержаны.");
      require(
          !n.hasAttribute("Geometry")
              || Set.of("Unknown", "Tetrahedral", "Linear", "TrigonalPlanar")
                  .contains(n.getAttribute("Geometry")),
          "Координационная геометрия пока не поддержана.");
      final int number = integer(attr(n, "Element", "6"));
      require(number > 0 && number <= 118, "Недопустимый атомный номер.");
      final IAtom a = new Atom(number);
      a.setFormalCharge(integer(attr(n, "Charge", "0")));
      if (n.hasAttribute("Isotope")) {
        final int mass = integer(n.getAttribute("Isotope"));
        require(mass > 0 && mass < 400, "Недопустимая масса изотопа.");
        a.setMassNumber(mass);
      }
      final var point = attr(n, "p", "").trim().split("\\s+");
      require(point.length == 2, "Для каждого атома нужны 2D-координаты.");
      final double x = Double.parseDouble(point[0]), y = Double.parseDouble(point[1]);
      require(
          Double.isFinite(x) && Double.isFinite(y) && Math.abs(x) < 1e7 && Math.abs(y) < 1e7,
          "Недопустимые координаты атома.");
      a.setPoint2d(new Point2d(x / 28, -y / 28));
      if (n.hasAttribute("NumHydrogens")) {
        final int h = integer(n.getAttribute("NumHydrogens"));
        require(h >= 0 && h <= 8, "Недопустимое число водородов.");
        hydrogens.put(graph.getAtomCount(), h);
        a.setImplicitHydrogenCount(h);
      }
      atoms.put(id(n), a);
      atomIds.put(id(n), AtomId.of("a" + graph.getAtomCount()));
      graph.addAtom(a);
    }
    require(!atoms.isEmpty(), "Пустой фрагмент.");
    final Set<String> pairs = new HashSet<>();
    for (final var b : children(f)) {
      if (!b.getTagName().equals("b")) {
        continue;
      }
      attributes(b, Set.of("B", "E", "Order", "Display", "Display2", "BS", "BondCircularOrdering"));
      require(
          !b.hasAttribute("Display2")
              || Set.of("Solid", "Dash").contains(b.getAttribute("Display2"))
                  && (!b.getAttribute("Display2").equals("Dash")
                      || attr(b, "Order", "1").equals("1.5")),
          "Нестандартное оформление двойной связи требует проверки.");
      final IAtom a = atoms.get(b.getAttribute("B")), z = atoms.get(b.getAttribute("E"));
      require(a != null && z != null && a != z, "Связь ссылается на отсутствующий атом.");
      final String pair =
          Math.min(graph.indexOf(a), graph.indexOf(z))
              + ":"
              + Math.max(graph.indexOf(a), graph.indexOf(z));
      require(pairs.add(pair), "Повторная связь между атомами.");
      final String order = attr(b, "Order", "1");
      require(
          Set.of("1", "2", "3", "1.5").contains(order),
          "Тип связи «" + order + "» пока не поддержан.");
      final IBond bond =
          new Bond(
              a,
              z,
              order.equals("2")
                  ? IBond.Order.DOUBLE
                  : order.equals("3") ? IBond.Order.TRIPLE : IBond.Order.SINGLE);
      if (order.equals("1.5")) {
        bond.setIsAromatic(true);
        a.setIsAromatic(true);
        z.setIsAromatic(true);
      }
      final String display = attr(b, "Display", "Solid");
      final IBond.Stereo stereo =
          switch (display) {
            case "Solid", "Bold" -> IBond.Stereo.NONE;
            case "Dash" -> {
              require(
                  order.equals("1.5"),
                  "Пунктирная связь без явного ароматического порядка требует проверки.");
              yield IBond.Stereo.NONE;
            }
            case "WedgeBegin" -> IBond.Stereo.UP;
            case "WedgeEnd" -> IBond.Stereo.UP_INVERTED;
            case "WedgedHashBegin" -> IBond.Stereo.DOWN;
            case "WedgedHashEnd" -> IBond.Stereo.DOWN_INVERTED;
            default ->
                throw new IllegalArgumentException(
                    "Связь «" + display + "» требует отдельного разбора.");
          };
      require(
          stereo == IBond.Stereo.NONE || order.equals("1"), "Клин должен быть одинарной связью.");
      bond.setStereo(stereo);
      wedge |= stereo != IBond.Stereo.NONE;
      graph.addBond(bond);
    }
    for (final IAtom a : graph.atoms()) {
      if (a.isAromatic() && a.getImplicitHydrogenCount() == null) {
        final int degree = graph.getConnectedBondsCount(a);
        require(degree >= 2 && degree <= 3, "Неопределённая ароматическая валентность.");
        a.setImplicitHydrogenCount(a.getAtomicNumber() == 6 ? 3 - degree : 0);
      }
    }
    if (graph.bonds().iterator().hasNext()) {
      org.openscience.cdk.aromaticity.Kekulization.kekulize(graph);
    }
    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(graph);
    final var adder =
        org.openscience.cdk.tools.CDKHydrogenAdder.getInstance(
            SilentChemObjectBuilder.getInstance());
    for (final IAtom a : graph.atoms()) {
      if (hydrogens.containsKey(graph.indexOf(a))) {
        a.setImplicitHydrogenCount(hydrogens.get(graph.indexOf(a)));
      } else {
        adder.addImplicitHydrogens(graph, a);
      }
    }
    graph.setStereoElements(
        org.openscience.cdk.stereo.StereoElementFactory.using2DCoordinates(graph)
            .checkSymmetry(true)
            .createAll());
    require(
        !wedge || graph.stereoElements().iterator().hasNext(),
        "Клинья не удалось согласовать со стереоцентрами.");
    for (final IBond b : graph.bonds()) {
      if (!Set.of(
              IBond.Stereo.UP,
              IBond.Stereo.DOWN,
              IBond.Stereo.UP_INVERTED,
              IBond.Stereo.DOWN_INVERTED)
          .contains(b.getStereo())) {
        continue;
      }
      final IAtom focus =
          b.getStereo() == IBond.Stereo.UP_INVERTED || b.getStereo() == IBond.Stereo.DOWN_INVERTED
              ? b.getEnd()
              : b.getBegin();
      boolean found = false;
      for (final var stereo : graph.stereoElements()) {
        if (stereo.getFocus() instanceof IAtom center
            && graph.indexOf(center) == graph.indexOf(focus)) {
          found = true;
        }
      }
      require(
          found,
          "Клин не относится к прочитанному тетраэдрическому центру; осевая стереохимия пока не поддержана.");
    }
    for (final var n : children(f)) {
      if (n.getTagName().equals("n") && Set.of("R", "S", "r", "s").contains(n.getAttribute("AS"))) {
        final IAtom focus = atoms.get(n.getAttribute("id"));
        boolean found = false;
        for (final var s : graph.stereoElements()) {
          if (s.getFocus() instanceof IAtom center
              && graph.indexOf(center) == graph.indexOf(focus)) {
            found = true;
          }
        }
        require(found, "Есть обозначение R/S без читаемой геометрии стереоцентра.");
      }
    }
    if (f.hasAttribute("Charge")) {
      require(
          integer(f.getAttribute("Charge"))
              == atoms.values().stream().mapToInt(IAtom::getFormalCharge).sum(),
          "Заряд фрагмента не равен сумме зарядов атомов.");
    }
    require(
        !f.hasAttribute("SpinMultiplicity") || integer(f.getAttribute("SpinMultiplicity")) == 1,
        "Незамкнутая электронная оболочка пока не поддержана.");
    final var entry = CdkStructureExchange.fromGraph(graph, Map.of(), source, 1, false);
    return new Fragment(id(f), title, entry, atomIds, "");
  }

  private static void expandNicknames(final Element fragment, final int depth) {
    require(depth < 20, "Слишком много вложенных сокращённых групп.");
    for (final Element n : new ArrayList<>(children(fragment))) {
      if (!n.getTagName().equals("n")
          || !Set.of("Nickname", "Fragment").contains(n.getAttribute("NodeType"))) {
        continue;
      }
      attributes(n, Set.of("NodeType", "AS", "BondOrdering"));
      final var embedded =
          children(n).stream().filter(e -> e.getTagName().equals("fragment")).toList();
      require(embedded.size() == 1, "Сокращение не содержит однозначной развёрнутой структуры.");
      final Element inner = embedded.getFirst();
      attributes(inner, Set.of("Name"));
      expandNicknames(inner, depth + 1);
      final var connectors =
          children(inner).stream()
              .filter(
                  e ->
                      e.getTagName().equals("n")
                          && e.getAttribute("NodeType").equals("ExternalConnectionPoint"))
              .toList();
      require(
          connectors.size() == 1,
          "Сокращённая группа с несколькими точками присоединения требует развёртки в ChemDraw.");
      final String connection = id(connectors.getFirst());
      final var exits =
          children(inner).stream()
              .filter(
                  e ->
                      e.getTagName().equals("b")
                          && (e.getAttribute("B").equals(connection)
                              || e.getAttribute("E").equals(connection)))
              .toList();
      require(exits.size() == 1, "Неоднозначная точка присоединения сокращённой группы.");
      final Element exit = exits.getFirst();
      require(
          attr(exit, "Order", "1").equals("1") && attr(exit, "Display", "Solid").equals("Solid"),
          "Стерео/кратная связь на точке присоединения требует ручной развёртки.");
      final String attachment =
          exit.getAttribute("B").equals(connection)
              ? exit.getAttribute("E")
              : exit.getAttribute("B");
      final String outer = id(n);
      final var bonds =
          children(fragment).stream()
              .filter(
                  e ->
                      e.getTagName().equals("b")
                          && (e.getAttribute("B").equals(outer)
                              || e.getAttribute("E").equals(outer)))
              .toList();
      require(
          bonds.size() == 1 && attr(bonds.getFirst(), "Order", "1").equals("1"),
          "Сокращение должно иметь одну одинарную внешнюю связь.");
      final Element bond = bonds.getFirst();
      final String display = attr(bond, "Display", "Solid");
      require(
          !(bond.getAttribute("B").equals(outer)
                  && Set.of("WedgeBegin", "WedgedHashBegin").contains(display))
              && !(bond.getAttribute("E").equals(outer)
                  && Set.of("WedgeEnd", "WedgedHashEnd").contains(display)),
          "Стереоцентр внутри сокращения требует явной развёртки.");
      bond.setAttribute(bond.getAttribute("B").equals(outer) ? "B" : "E", attachment);
      for (final Element child : new ArrayList<>(children(inner))) {
        if (child == connectors.getFirst() || child == exit) {
          continue;
        }
        fragment.appendChild(child);
      }
      fragment.removeChild(n);
      require(all(fragment, "n").size() <= 1000, "После развёртки больше 1000 атомов.");
    }
  }

  private static Step step(
      final Element s,
      final String title,
      final Map<String, Element> objects,
      final Map<String, Fragment> fragments) {
    attributes(
        s,
        Set.of(
            "Name",
            "ReactionStepReactants",
            "ReactionStepProducts",
            "ReactionStepObjectsAboveArrow",
            "ReactionStepObjectsBelowArrow",
            "ReactionStepArrows",
            "ReactionStepPlusses",
            "ReactionStepAtomMap",
            "ReactionStepAtomMapManual",
            "ReactionStepAtomMapAuto"));
    final Map<String, ReactionComponentRole> roles = new LinkedHashMap<>();
    final List<String> note = new ArrayList<>();
    for (final String key :
        List.of(
            "ReactionStepReactants",
            "ReactionStepProducts",
            "ReactionStepObjectsAboveArrow",
            "ReactionStepObjectsBelowArrow")) {
      for (final String ref : expanded(s, key, objects)) {
        final var f = fragments.get(ref);
        if (f == null && key.contains("Arrow")) {
          final var obj = objects.get(ref);
          require(
              obj != null && obj.getTagName().equals("t"),
              "Неразобранный объект над/под стрелкой: " + ref);
          final String text = caption(obj);
          if (!text.isBlank()) {
            note.add(text);
          }
          continue;
        }
        require(
            f != null && f.supported(),
            "Участник " + ref + " не прочитан. Сначала исправьте его в исходнике.");
        final var role =
            key.equals("ReactionStepReactants")
                ? ReactionComponentRole.REACTANT
                : key.equals("ReactionStepProducts")
                    ? ReactionComponentRole.PRODUCT
                    : ReactionComponentRole.OTHER;
        require(
            roles.putIfAbsent(ref, role) == null,
            "Один фрагмент повторяется в стадии. Используйте отдельные копии по сторонам реакции.");
      }
    }
    require(
        roles.containsValue(ReactionComponentRole.REACTANT)
            && roles.containsValue(ReactionComponentRole.PRODUCT),
        "Стадии нужны исходник и продукт.");
    final Map<String, Map<AtomId, Integer>> maps = new LinkedHashMap<>();
    roles.keySet().forEach(k -> maps.put(k, new LinkedHashMap<>()));
    final Set<String> pairSet = new LinkedHashSet<>();
    final Map<String, String> paired = new HashMap<>();
    int label = 0;
    for (final String name :
        List.of("ReactionStepAtomMap", "ReactionStepAtomMapManual", "ReactionStepAtomMapAuto")) {
      final var refs = refs(s, name);
      require(refs.size() % 2 == 0, "Атомная карта должна содержать пары атомов.");
      for (int i = 0; i < refs.size(); i += 2) {
        final String a = refs.get(i), b = refs.get(i + 1);
        if (!pairSet.add(a + ":" + b)) {
          continue;
        }
        require(
            paired.putIfAbsent(a, b) == null && paired.putIfAbsent(b, a) == null,
            "Атомная карта содержит противоречивые пары.");
        final String left = owner(a, roles, fragments, ReactionComponentRole.REACTANT),
            right = owner(b, roles, fragments, ReactionComponentRole.PRODUCT);
        final int number = ++label;
        maps.get(left).put(fragments.get(left).atomIds().get(a), number);
        maps.get(right).put(fragments.get(right).atomIds().get(b), number);
      }
    }
    LaboratoryReactionStage.Arrow arrow = LaboratoryReactionStage.Arrow.FORWARD;
    for (final String ref : refs(s, "ReactionStepArrows")) {
      final var a = objects.get(ref);
      require(a != null, "Стрелка стадии не найдена.");
      final String type = attr(a, "ArrowType", "FullHead");
      require(
          Set.of("FullHead", "Equilibrium", "Solid").contains(type),
          "Тип стрелки «" + type + "» пока не поддержан.");
      if (type.equals("Equilibrium")) {
        arrow = LaboratoryReactionStage.Arrow.EQUILIBRIUM;
      }
    }
    final List<Member> members = new ArrayList<>();
    roles.forEach((id, role) -> members.add(new Member(id, role, maps.get(id))));
    return new Step(id(s), title, members, arrow, String.join("\n", note), "");
  }

  private static List<String> expanded(
      final Element step, final String key, final Map<String, Element> objects) {
    final List<String> result = new ArrayList<>();
    for (final String ref : refs(step, key)) {
      final Element object = objects.get(ref);
      if (object != null && object.getTagName().equals("group")) {
        final var fragments =
            all(object, "fragment").stream().filter(f -> !hasAncestor(f, "n")).toList();
        require(!fragments.isEmpty(), "В группе нет химических фрагментов.");
        fragments.forEach(f -> result.add(id(f)));
      } else {
        result.add(ref);
      }
    }
    return result;
  }

  private static String owner(
      final String atom,
      final Map<String, ReactionComponentRole> roles,
      final Map<String, Fragment> fragments,
      final ReactionComponentRole role) {
    return roles.keySet().stream()
        .filter(k -> roles.get(k) == role && fragments.get(k).atomIds().containsKey(atom))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Атом карты " + atom + " не относится к нужной стороне реакции."));
  }

  private static String escape(final String s) {
    require(
        s != null
            && s.codePoints()
                .allMatch(
                    c ->
                        c == 9
                            || c == 10
                            || c == 13
                            || c >= 32 && c <= 0xD7FF
                            || c >= 0xE000 && c <= 0xFFFD
                            || c >= 0x10000 && c <= 0x10FFFF),
        "В названии есть недопустимый символ XML.");
    return s.replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "&#10;")
        .replace("\r", "&#13;");
  }

  private static String number(final double n) {
    return String.format(Locale.ROOT, "%.4f", n);
  }

  private static final class Writer {
    int next = 10;
    double y = 70;
    final StringBuilder body = new StringBuilder();
    final List<String> warnings = new ArrayList<>();

    int id() {
      return this.next++;
    }

    record Placed(int id, double width, Map<AtomId, Integer> atoms) {}

    Placed fragment(final MoleculeDrawing drawing, final double x, final double y)
        throws Exception {
      final var full = CdkStructureExchange.project(drawing);
      for (int i = 0; i < full.getAtomCount(); i++) {
        full.getAtom(i).setProperty("quantum.cdxml.atom", drawing.molecule().atoms().get(i).id());
      }
      final var graph = AtomContainerManipulator.suppressHydrogens(full);
      new StructureDiagramGenerator().generateCoordinates(graph);
      final int fragment = this.id();
      final Map<IAtom, Integer> ids = new IdentityHashMap<>();
      final Map<AtomId, Integer> coreIds = new LinkedHashMap<>();
      double minX = Double.POSITIVE_INFINITY,
          maxX = Double.NEGATIVE_INFINITY,
          minY = Double.POSITIVE_INFINITY,
          maxY = Double.NEGATIVE_INFINITY;
      for (final IAtom a : graph.atoms()) {
        final var p = a.getPoint2d();
        minX = Math.min(minX, p.x * 28);
        maxX = Math.max(maxX, p.x * 28);
        minY = Math.min(minY, -p.y * 28);
        maxY = Math.max(maxY, -p.y * 28);
      }
      this.body
          .append("<fragment id=\"")
          .append(fragment)
          .append("\" Name=\"")
          .append(escape(drawing.molecule().displayName()))
          .append("\">\n");
      for (final IAtom a : graph.atoms()) {
        final int node = this.id();
        ids.put(a, node);
        final AtomId atomId = a.getProperty("quantum.cdxml.atom");
        if (atomId != null) {
          coreIds.put(atomId, node);
        }
        final var p = a.getPoint2d();
        this.body
            .append("<n id=\"")
            .append(node)
            .append("\" p=\"")
            .append(number(x + p.x * 28 - minX))
            .append(' ')
            .append(number(y - p.y * 28 - minY))
            .append("\" Element=\"")
            .append(a.getAtomicNumber())
            .append("\" NumHydrogens=\"")
            .append(a.getImplicitHydrogenCount() == null ? 0 : a.getImplicitHydrogenCount())
            .append("\" Charge=\"")
            .append(a.getFormalCharge() == null ? 0 : a.getFormalCharge())
            .append('"');
        if (a.getMassNumber() != null) {
          this.body.append(" Isotope=\"").append(a.getMassNumber()).append('"');
        }
        this.body.append(">\n");
        if (a.getAtomicNumber() != 6) {
          this.body.append("<t><s font=\"3\" size=\"10\" face=\"96\">").append(a.getSymbol());
          final int h = a.getImplicitHydrogenCount() == null ? 0 : a.getImplicitHydrogenCount();
          if (h > 0) {
            this.body.append('H');
            if (h > 1) {
              this.body.append(h);
            }
          }
          this.body.append("</s></t>\n");
        }
        this.body.append("</n>\n");
      }
      for (final IBond b : graph.bonds()) {
        final String display =
            switch (b.getStereo()) {
              case UP -> "WedgeBegin";
              case UP_INVERTED -> "WedgeEnd";
              case DOWN -> "WedgedHashBegin";
              case DOWN_INVERTED -> "WedgedHashEnd";
              case NONE, E, Z, E_Z_BY_COORDINATES -> "Solid";
              default ->
                  throw new IllegalArgumentException(
                      "Неопределённая стереосвязь не экспортируется.");
            };
        this.body
            .append("<b id=\"")
            .append(this.id())
            .append("\" B=\"")
            .append(ids.get(b.getBegin()))
            .append("\" E=\"")
            .append(ids.get(b.getEnd()))
            .append("\" Order=\"")
            .append(b.getOrder().numeric())
            .append("\" Display=\"")
            .append(display)
            .append("\"/>\n");
      }
      this.body.append("</fragment>\n");
      this.text(drawing.molecule().displayName(), x, y + maxY - minY + 28);
      return new Placed(fragment, Math.max(140, maxX - minX + 65), coreIds);
    }

    int text(final String text, final double x, final double y) {
      final int id = this.id();
      this.body
          .append("<t id=\"")
          .append(id)
          .append("\" p=\"")
          .append(number(x))
          .append(' ')
          .append(number(y))
          .append("\"><s font=\"3\" size=\"10\">")
          .append(escape(text))
          .append("</s></t>\n");
      return id;
    }
  }

  @Override
  public Export write(
      final LaboratoryDrawing drawing, final Set<String> componentIds, final Set<String> stageIds) {
    require(!componentIds.isEmpty() || !stageIds.isEmpty(), "Выберите вещества или стадии.");
    require(componentIds.size() <= 100 && stageIds.size() <= 100, "Не более 100 веществ и стадий.");
    final Writer w = new Writer();
    final Set<String> used = new HashSet<>();
    final Map<String, LaboratoryComponent> components = new LinkedHashMap<>();
    drawing.experiment().components().forEach(c -> components.put(c.id(), c));
    try {
      final var stages =
          drawing.experiment().stages().stream().filter(s -> stageIds.contains(s.id())).toList();
      require(stages.size() == stageIds.size(), "Стадия не найдена.");
      int count = 0;
      for (final var stage : stages) {
        cancelled();
        final var problems = stage.atomMapping().problems(drawing.experiment(), stage);
        require(problems.isEmpty(), String.join(" ", problems));
        final Map<String, Writer.Placed> placed = new LinkedHashMap<>();
        final List<Integer> left = new ArrayList<>(),
            right = new ArrayList<>(),
            agents = new ArrayList<>();
        double x = 40;
        w.text(stage.title(), 40, w.y - 25);
        for (final var side :
            List.of(
                ReactionComponentRole.REACTANT,
                ReactionComponentRole.PRODUCT,
                ReactionComponentRole.OTHER)) {
          for (final var member : stage.participants()) {
            if ((side == ReactionComponentRole.OTHER
                ? member.role() != ReactionComponentRole.REACTANT
                    && member.role() != ReactionComponentRole.PRODUCT
                : member.role() == side)) {
              require(
                  ++count <= 100, "Экспорт создаёт больше 100 фрагментов. Выберите меньше стадий.");
              final var p =
                  w.fragment(
                      drawing.layouts().get(member.componentId()),
                      x,
                      w.y + (side == ReactionComponentRole.OTHER ? 160 : 0));
              placed.put(member.componentId(), p);
              (side == ReactionComponentRole.REACTANT
                      ? left
                      : side == ReactionComponentRole.PRODUCT ? right : agents)
                  .add(p.id());
              used.add(member.componentId());
              x += p.width();
              if (member.coefficient() != null) {
                w.warnings.add(
                    "Коэффициенты стадий не переносятся; исходный опыт сохраните в QCHEM.");
              }
            }
          }
          if (side == ReactionComponentRole.REACTANT) {
            x += 110;
          }
        }
        require(!left.isEmpty() && !right.isEmpty(), "Стадии нужны исходник и продукт.");
        final double arrowX =
            40
                + placed.entrySet().stream()
                    .filter(e -> left.contains(e.getValue().id()))
                    .mapToDouble(e -> e.getValue().width())
                    .sum();
        final int arrow = w.id();
        w.body
            .append("<graphic id=\"")
            .append(arrow)
            .append("\" GraphicType=\"Line\" ArrowType=\"")
            .append(
                stage.arrow() == LaboratoryReactionStage.Arrow.EQUILIBRIUM
                    ? "Equilibrium"
                    : "FullHead")
            .append("\" BoundingBox=\"")
            .append(number(arrowX + 80))
            .append(' ')
            .append(number(w.y + 35))
            .append(' ')
            .append(number(arrowX))
            .append(' ')
            .append(number(w.y + 35))
            .append("\"/>\n");
        final StringBuilder pairs = new StringBuilder();
        final var labels = stage.atomMapping().labels();
        final Map<Integer, List<LaboratoryAtomMapping.Label>> grouped = new TreeMap<>();
        labels.forEach(l -> grouped.computeIfAbsent(l.number(), k -> new ArrayList<>()).add(l));
        for (final var entry : grouped.entrySet()) {
          final var pair = entry.getValue();
          require(
              pair.size() == 2,
              "CDXML требует пары для каждой метки карты; непарные метки сохраните в RXN/QCHEM.");
          LaboratoryAtomMapping.Label a = null, b = null;
          for (final var label : pair) {
            final var role =
                stage.participants().stream()
                    .filter(p -> p.componentId().equals(label.componentId()))
                    .findFirst()
                    .orElseThrow()
                    .role();
            if (role == ReactionComponentRole.REACTANT) {
              a = label;
            } else if (role == ReactionComponentRole.PRODUCT) {
              b = label;
            }
          }
          require(a != null && b != null, "Для пары карты нужны исходник и продукт.");
          final Integer first = placed.get(a.componentId()).atoms().get(a.atomId()),
              last = placed.get(b.componentId()).atoms().get(b.atomId());
          require(
              first != null && last != null,
              "Атом карты был свёрнут в подпись H. Для этой карты используйте RXN/QCHEM.");
          pairs.append(first).append(' ').append(last).append(' ');
        }
        final List<Integer> above = new ArrayList<>(agents);
        final var condition = stage.conditions();
        if (condition.note() != null && !condition.note().isBlank()) {
          above.add(w.text(condition.note(), arrowX, w.y + 90));
        }
        if (!condition.equals(
            ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions.EMPTY)) {
          w.warnings.add(
              "Текстовая заметка условий переносится. Структурированные температура, длительность, давление и признаки остаются в QCHEM.");
        }
        w.body
            .append("<scheme id=\"")
            .append(w.id())
            .append("\"><step id=\"")
            .append(w.id())
            .append("\" Name=\"")
            .append(escape(stage.title()))
            .append("\" ReactionStepReactants=\"")
            .append(join(left))
            .append("\" ReactionStepProducts=\"")
            .append(join(right))
            .append("\" ReactionStepObjectsAboveArrow=\"")
            .append(join(above))
            .append("\" ReactionStepArrows=\"")
            .append(arrow)
            .append("\" ReactionStepAtomMap=\"")
            .append(pairs.toString().strip())
            .append("\"/></scheme>\n");
        w.y += 400;
      }
      for (final String id : componentIds) {
        require(components.containsKey(id), "Вещество не найдено.");
        if (used.add(id)) {
          require(++count <= 100, "Не более 100 фрагментов в файле.");
          w.fragment(drawing.layouts().get(id), 40, w.y);
          w.y += 240;
        }
      }
      w.warnings.add(
          "CDXML содержит структуры, имена, явные стадии и парные атомные соответствия. Количества, выходы, анализы и журнал сохраняйте в QCHEM. Исходные номера атомных карт могут быть перенумерованы.");
      w.warnings.add(
          "Роли вспомогательных веществ передаются общей ролью «Другое». Общий промежуточный продукт разных стадий получает отдельные рисунки.");
      final String text =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE CDXML SYSTEM \"http://www.cambridgesoft.com/xml/cdxml.dtd\">\n<CDXML CreationProgram=\"Quantum IR Studio\" BondLength=\"28\" LabelFont=\"3\" CaptionFont=\"3\"><fonttable><font id=\"3\" charset=\"utf-8\" name=\"Arial\"/></fonttable><page id=\"1\">\n"
              + w.body
                  .toString()
                  .replace(" ReactionStepAtomMap=\"\"", "")
                  .replace(" ReactionStepObjectsAboveArrow=\"\"", "")
              + "</page></CDXML>\n";
      require(
          text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= MAX_BYTES,
          "Экспорт больше 8 МБ.");
      return new Export(text, w.warnings.stream().distinct().toList());
    } catch (final Exception e) {
      throw new IllegalArgumentException("CDXML: " + message(e), e);
    }
  }

  private static String join(final List<Integer> ids) {
    return String.join(" ", ids.stream().map(Object::toString).toList());
  }
}
