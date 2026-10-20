/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.StructureExchange;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.LaboratoryIdentityService;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStructureExchange;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;

/** Local name interpretation retains parser warnings and the existing graph/stereo conversion safeguards. */
public final class OpsinChemicalName {
  public record Result(
      String inputName,
      String smiles,
      MoleculeDrawing drawing,
      List<String> notes,
      boolean warning) {
    public Result {
      notes = List.copyOf(notes);
    }
  }

  public Result parse(final String input) {
    final String name = Objects.requireNonNull(input).strip();
    if (name.isBlank()
        || name.length() > 2000
        || name.indexOf('\n') >= 0
        || name.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("Введите одно химическое название длиной до 2000 знаков.");
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
    final var result = NameToStructure.getInstance().parseChemicalName(name);
    final String smiles = result.getSmiles();
    if (smiles == null) {
      throw new IllegalArgumentException(
          "Название не распознано: "
              + result.getMessage()
              + ". OPSIN распознаёт английскую химическую номенклатуру; проверьте локанты и скобки.");
    }
    final var entry =
        new CdkStructureExchange()
            .read(
                smiles + " " + name,
                StructureExchange.Format.SMILES,
                "OPSIN " + NameToStructure.getVersion() + "; введённое название: " + name)
            .getFirst();
    final String provenance =
        "Введённое пользователем название: "
            + name
            + "\nЛокальное распознавание OPSIN "
            + NameToStructure.getVersion()
            + ". Введённое имя сохранено как синоним; это не автоматически подтверждённое предпочтительное название IUPAC.";
    final var drawing = LaboratoryIdentityService.enteredName(entry.drawing(), name, provenance);
    final List<String> notes = new ArrayList<>(entry.notes());
    final boolean warning = !result.getStatus().name().equals("SUCCESS");
    if (warning) {
      notes.add("Предупреждение OPSIN: " + result.getMessage());
    }
    notes.add(
        "Название интерпретировано локально OPSIN "
            + NameToStructure.getVersion()
            + ". Введённое имя сохраняется как синоним с источником, а не как автоматически подтверждённое предпочтительное название IUPAC.");
    return new Result(name, smiles, drawing, notes, warning);
  }
}
