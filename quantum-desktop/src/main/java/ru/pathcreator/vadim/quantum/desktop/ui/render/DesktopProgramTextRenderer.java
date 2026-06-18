/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.render;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.application.persistence.result.QuantumIrWriteResult;
import ru.pathcreator.vadim.quantum.application.resource.ResourceEstimate;
import ru.pathcreator.vadim.quantum.desktop.workspace.DesktopIrOperationSpec;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Рендерит program-level текстовые панели для desktop workbench.
 */
public final class DesktopProgramTextRenderer {

    public String renderInspector(
        final String circuitName,
        final String quantumRegisterName,
        final int quantumRegisterSize,
        final String classicalRegisterName,
        final int classicalRegisterSize,
        final List<DesktopIrOperationSpec> operations,
        final List<String> gates,
        final IntegrationFormat targetFormat,
        final boolean russian
    ) {
        final StringBuilder text = new StringBuilder();
        text.append(russian ? "Программа" : "Program").append(System.lineSeparator());
        text.append("  ").append(russian ? "схема" : "circuit").append(": ").append(circuitName).append(System.lineSeparator());
        text.append("  ").append(russian ? "квантовый регистр" : "quantum register").append(": ").append(quantumRegisterName).append("[")
            .append(quantumRegisterSize).append("]").append(System.lineSeparator());
        text.append("  ").append(russian ? "классический регистр" : "classical register").append(": ").append(classicalRegisterName).append("[")
            .append(classicalRegisterSize).append("]").append(System.lineSeparator());
        text.append("  ").append(russian ? "операции" : "operations").append(": ").append(operations.size()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append(russian ? "Гистограмма gates" : "Gate histogram").append(System.lineSeparator());
        for (int i = 0; i < gates.size(); i++) {
            final String gate = gates.get(i);
            long count = 0L;
            for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
                if (gate.equals(operations.get(operationIndex).gate())) {
                    count++;
                }
            }
            if (count > 0) {
                text.append("  ").append(gate).append(": ").append(count).append(System.lineSeparator());
            }
        }
        text.append(System.lineSeparator());
        text.append(russian ? "Измерения" : "Measurements").append(System.lineSeparator());
        for (int i = 0; i < operations.size(); i++) {
            final DesktopIrOperationSpec operation = operations.get(i);
            if ("MEASURE".equals(operation.gate())) {
                text.append("  #").append(i).append(" ")
                    .append(operation.primaryQubit())
                    .append(" -> ")
                    .append(operation.classicalBit())
                    .append(System.lineSeparator());
            }
        }
        text.append(System.lineSeparator());
        text.append(russian ? "Цель" : "Target").append(System.lineSeparator());
        text.append("  ").append(russian ? "формат экспорта" : "export target").append(": ").append(targetFormat).append(System.lineSeparator());
        return text.toString();
    }

    public String renderOverview(
        final boolean activeJsonProgram,
        final String circuitName,
        final String experienceMode,
        final String layoutMode,
        final String wireOrder,
        final IntegrationFormat targetFormat,
        final QuantumIrWriteResult writeResult,
        final ValidationResult validation,
        final ResourceEstimate resources,
        final String preflightStatus,
        final int preflightDiagnostics,
        final boolean russian
    ) {
        final StringBuilder text = new StringBuilder();
        text.append(russian ? "Рабочая область" : "Workspace").append(System.lineSeparator());
        text.append("  ").append(russian ? "источник" : "source").append(": ").append(activeJsonProgram
            ? (russian ? "применён native JSON" : "applied native JSON")
            : (russian ? "графический native builder" : "graphical native builder")).append(System.lineSeparator());
        text.append("  ").append(russian ? "схема" : "circuit").append(": ").append(circuitName).append(System.lineSeparator());
        text.append("  ").append(russian ? "режим" : "mode").append(": ").append(experienceMode).append(System.lineSeparator());
        text.append("  ").append(russian ? "раскладка" : "layout").append(": ").append(layoutMode).append(System.lineSeparator());
        text.append("  ").append(russian ? "порядок линий" : "wire order").append(": ").append(wireOrder).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append(russian ? "Состояние программы" : "Program health").append(System.lineSeparator());
        text.append("  ").append(russian ? "валидация" : "validation").append(": ")
            .append(validation.isValid() ? (russian ? "валидна" : "valid") : (russian ? "невалидна" : "invalid"))
            .append(System.lineSeparator());
        text.append("  ").append(russian ? "ошибки валидации" : "validation errors").append(": ")
            .append(validation.errorCount()).append(System.lineSeparator());
        text.append("  json: ").append(writeResult.hasContent() ? (russian ? "готов" : "ready") : (russian ? "не готов" : "not ready"))
            .append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append(russian ? "Ресурсы" : "Resources").append(System.lineSeparator());
        text.append("  ").append(russian ? "схемы" : "circuits").append(": ").append(resources.circuitCount()).append(System.lineSeparator());
        text.append("  qubits: ").append(resources.qubitCount()).append(System.lineSeparator());
        text.append("  ").append(russian ? "классические биты" : "classical bits").append(": ").append(resources.classicalBitCount()).append(System.lineSeparator());
        text.append("  ").append(russian ? "операции" : "operations").append(": ").append(resources.operationCount()).append(System.lineSeparator());
        text.append("  gates: ").append(resources.gateCount()).append(System.lineSeparator());
        text.append("  ").append(russian ? "измерения" : "measurements").append(": ").append(resources.measurementCount()).append(System.lineSeparator());
        text.append("  ").append(russian ? "локальная симуляция" : "local simulation").append(": ")
            .append(resources.isLocalSimulationFeasible() ? (russian ? "доступна" : "feasible") : (russian ? "слишком большая" : "too large"))
            .append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append(russian ? "Цель" : "Target").append(System.lineSeparator());
        text.append("  ").append(russian ? "формат" : "format").append(": ").append(targetFormat).append(System.lineSeparator());
        text.append("  preflight: ").append(preflightStatus).append(System.lineSeparator());
        text.append("  ").append(russian ? "диагностика" : "diagnostics").append(": ").append(preflightDiagnostics).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append(russian ? "Гистограмма gates" : "Gate histogram").append(System.lineSeparator());
        final List<String> gateNames = List.copyOf(resources.gateHistogram().keySet());
        for (int i = 0; i < gateNames.size(); i++) {
            final String gate = gateNames.get(i);
            text.append("  ")
                .append(gate)
                .append(": ")
                .append(resources.gateHistogram().get(gate))
                .append(System.lineSeparator());
        }
        return text.toString();
    }

    public String renderAssistantNotes(
        final boolean activeJsonProgram,
        final ValidationResult validation,
        final ResourceEstimate resources,
        final String preflightStatus,
        final IntegrationFormat targetFormat,
        final boolean russian
    ) {
        final StringBuilder text = new StringBuilder();
        text.append(russian ? "Заметки помощника" : "Assistant Notes").append(System.lineSeparator());
        text.append(System.lineSeparator());
        if (activeJsonProgram) {
            text.append(russian
                ? "- Применённый native JSON сейчас активен. Графические операции остаются черновиком до редактирования."
                : "- Applied native JSON is the active program. Graphical operations remain available as a draft until edited.")
                .append(System.lineSeparator());
        }
        if (validation.isValid()) {
            text.append(russian
                ? "- Текущая native IR программа проходит валидацию."
                : "- The current native IR program validates successfully.").append(System.lineSeparator());
        } else {
            text.append(russian
                ? "- Исправьте ошибки валидации перед симуляцией или экспортом. Ошибок: "
                : "- Fix validation errors before simulation/export. Error count: ")
                .append(validation.errorCount())
                .append(System.lineSeparator());
        }
        if (resources.operationCount() >= 50) {
            text.append(russian
                ? "- Активен режим большой схемы. Используйте горизонтальную прокрутку и индексы шагов."
                : "- Large circuit UI mode is active. Use horizontal scroll and step indices to inspect operations.")
                .append(System.lineSeparator());
        }
        if (!resources.isLocalSimulationFeasible()) {
            text.append(russian
                ? "- Локальная state-vector симуляция недоступна при текущем лимите qubits."
                : "- Local state-vector simulation is not feasible with the current qubit limit.")
                .append(System.lineSeparator());
        } else {
            text.append(russian
                ? "- Локальная симуляция доступна для этого размера схемы."
                : "- Local simulation is feasible for this circuit size.").append(System.lineSeparator());
        }
        if ("EXPORTABLE".equals(preflightStatus)) {
            text.append(russian ? "- Экспорт в " : "- Target export should be direct for ")
                .append(targetFormat)
                .append(russian ? " должен быть прямым." : ".")
                .append(System.lineSeparator());
        } else if ("LOWERING_REQUIRED".equals(preflightStatus)) {
            text.append(russian
                ? "- Для экспорта нужен lowering. Проверьте Transform/Preflight перед использованием результата."
                : "- Target export needs lowering. Inspect Transform/Preflight before relying on generated text.")
                .append(System.lineSeparator());
        } else {
            text.append(russian
                ? "- У экспорта есть ограничения. Проверьте Preflight diagnostics."
                : "- Target export has restrictions. Inspect Preflight diagnostics before export.")
                .append(System.lineSeparator());
        }
        text.append(russian
            ? "- Информация выбранного gate обновляется во вкладке Gate Wiki; используйте поиск gate для фильтра."
            : "- Selected gate info updates in the Gate Info tab; use Find gate to filter the catalog.")
            .append(System.lineSeparator());
        return text.toString();
    }
}