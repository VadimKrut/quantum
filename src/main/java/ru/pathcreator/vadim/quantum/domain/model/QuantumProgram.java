/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.model;

import java.util.ArrayList;
import java.util.List;

import ru.pathcreator.vadim.quantum.domain.calibration.CalibrationDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.CallableDefinition;
import ru.pathcreator.vadim.quantum.domain.callable.ExternalCallableDeclaration;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalDeclaration;
import ru.pathcreator.vadim.quantum.domain.gate.GateDefinition;

/**
 * РљРІР°РЅС‚РѕРІР°СЏ РїСЂРѕРіСЂР°РјРјР°, СЃРѕРґРµСЂР¶Р°С‰Р°СЏ РІС‹С‡РёСЃР»РёС‚РµР»СЊРЅСѓСЋ РјРѕРґРµР»СЊ Рё РЅР°Р±РѕСЂ СЃС…РµРј.
 */
public final class QuantumProgram {

    /**
     * Р’С‹С‡РёСЃР»РёС‚РµР»СЊРЅР°СЏ РјРѕРґРµР»СЊ РїСЂРѕРіСЂР°РјРјС‹.
     */
    private final QuantumComputationModel computationModel;

    /**
     * РЎС…РµРјС‹, СЃРѕР·РґР°РЅРЅС‹Рµ РІРЅСѓС‚СЂРё РїСЂРѕРіСЂР°РјРјС‹.
     */
    private final ArrayList<QuantumCircuit> circuits;

    /**
     * РџРѕР»СЊР·РѕРІР°С‚РµР»СЊСЃРєРёРµ gate definitions, РґРѕСЃС‚СѓРїРЅС‹Рµ СЃС…РµРјР°Рј РїСЂРѕРіСЂР°РјРјС‹.
     */
    private final ArrayList<GateDefinition> gateDefinitions;
    private final ArrayList<ClassicalDeclaration> classicalDeclarations;
    private final ArrayList<CallableDefinition> callableDefinitions;
    private final ArrayList<ExternalCallableDeclaration> externalCallableDeclarations;
    private final ArrayList<CalibrationDefinition> calibrationDefinitions;

    private QuantumProgram(final QuantumComputationModel computationModel) {
        this.computationModel = computationModel;
        this.circuits = new ArrayList<>();
        this.gateDefinitions = new ArrayList<>();
        this.classicalDeclarations = new ArrayList<>();
        this.callableDefinitions = new ArrayList<>();
        this.externalCallableDeclarations = new ArrayList<>();
        this.calibrationDefinitions = new ArrayList<>();
    }

    /**
     * РЎРѕР·РґР°РµС‚ РїСЂРѕРіСЂР°РјРјСѓ РґР»СЏ gate-based quantum circuits.
     *
     * @return gate-based РєРІР°РЅС‚РѕРІР°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public static QuantumProgram gateBased() {
        return create(QuantumComputationModel.GATE_BASED_CIRCUIT);
    }

    /**
     * РЎРѕР·РґР°РµС‚ РїСЂРѕРіСЂР°РјРјСѓ РґР»СЏ СѓРєР°Р·Р°РЅРЅРѕР№ РІС‹С‡РёСЃР»РёС‚РµР»СЊРЅРѕР№ РјРѕРґРµР»Рё.
     *
     * @param computationModel РІС‹С‡РёСЃР»РёС‚РµР»СЊРЅР°СЏ РјРѕРґРµР»СЊ РїСЂРѕРіСЂР°РјРјС‹
     * @return РєРІР°РЅС‚РѕРІР°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public static QuantumProgram create(final QuantumComputationModel computationModel) {
        if (computationModel == null) {
            throw new IllegalArgumentException("Quantum computation model must not be null.");
        }
        return new QuantumProgram(computationModel);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РІС‹С‡РёСЃР»РёС‚РµР»СЊРЅСѓСЋ РјРѕРґРµР»СЊ РїСЂРѕРіСЂР°РјРјС‹.
     *
     * @return РІС‹С‡РёСЃР»РёС‚РµР»СЊРЅР°СЏ РјРѕРґРµР»СЊ
     */
    public QuantumComputationModel computationModel() {
        return computationModel;
    }

    /**
     * РЎРѕР·РґР°РµС‚ СЃС…РµРјСѓ РІРЅСѓС‚СЂРё РїСЂРѕРіСЂР°РјРјС‹.
     *
     * @param name РёРјСЏ СЃС…РµРјС‹
     * @return СЃРѕР·РґР°РЅРЅР°СЏ СЃС…РµРјР°
     */
    public QuantumCircuit createCircuit(final String name) {
        ensureGateBased();
        final QuantumCircuit circuit = QuantumCircuit.create(
            this,
            CircuitName.of(name)
        );
        circuits.add(circuit);
        return circuit;
    }

    /**
     * Р”РѕР±Р°РІР»СЏРµС‚ gate definition РІ РїСЂРѕРіСЂР°РјРјСѓ.
     *
     * @param definition РѕРїРёСЃР°РЅРёРµ РіРµР№С‚Р°
     * @return С‚РµРєСѓС‰Р°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public QuantumProgram addGateDefinition(final GateDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Gate definition must not be null.");
        }
        ensureUniqueProgramSymbol(definition.gateName());
        gateDefinitions.add(definition);
        return this;
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєРѕР»РёС‡РµСЃС‚РІРѕ gate definitions.
     *
     * @return РєРѕР»РёС‡РµСЃС‚РІРѕ gate definitions
     */
    public int gateDefinitionCount() {
        return gateDefinitions.size();
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ gate definition РїРѕ РёРЅРґРµРєСЃСѓ.
     *
     * @param index РёРЅРґРµРєСЃ gate definition
     * @return gate definition
     */
    public GateDefinition gateDefinition(final int index) {
        validateGateDefinitionIndex(index);
        return gateDefinitions.get(index);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РЅРµРёР·РјРµРЅСЏРµРјС‹Р№ СЃРЅРёРјРѕРє РѕРїРёСЃР°РЅРёР№ РіРµР№С‚РѕРІ.
     *
     * @return СЃРїРёСЃРѕРє gate definitions
     */
    public List<GateDefinition> gateDefinitions() {
        return List.copyOf(gateDefinitions);
    }

    /**
     * Р”РѕР±Р°РІР»СЏРµС‚ РєР»Р°СЃСЃРёС‡РµСЃРєРѕРµ РѕР±СЉСЏРІР»РµРЅРёРµ СѓСЂРѕРІРЅСЏ РїСЂРѕРіСЂР°РјРјС‹.
     *
     * @param declaration РєР»Р°СЃСЃРёС‡РµСЃРєРѕРµ РѕР±СЉСЏРІР»РµРЅРёРµ
     * @return С‚РµРєСѓС‰Р°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public QuantumProgram addClassicalDeclaration(final ClassicalDeclaration declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("Classical declaration must not be null.");
        }
        ensureUniqueProgramSymbol(declaration.name());
        classicalDeclarations.add(declaration);
        return this;
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєРѕР»РёС‡РµСЃС‚РІРѕ РєР»Р°СЃСЃРёС‡РµСЃРєРёС… РѕР±СЉСЏРІР»РµРЅРёР№.
     *
     * @return РєРѕР»РёС‡РµСЃС‚РІРѕ РѕР±СЉСЏРІР»РµРЅРёР№
     */
    public int classicalDeclarationCount() {
        return classicalDeclarations.size();
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєР»Р°СЃСЃРёС‡РµСЃРєРѕРµ РѕР±СЉСЏРІР»РµРЅРёРµ РїРѕ РёРЅРґРµРєСЃСѓ.
     *
     * @param index РёРЅРґРµРєСЃ РѕР±СЉСЏРІР»РµРЅРёСЏ
     * @return РєР»Р°СЃСЃРёС‡РµСЃРєРѕРµ РѕР±СЉСЏРІР»РµРЅРёРµ
     */
    public ClassicalDeclaration classicalDeclaration(final int index) {
        validateIndex(
            index,
            classicalDeclarations.size(),
            "Classical declaration"
        );
        return classicalDeclarations.get(index);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РЅРµРёР·РјРµРЅСЏРµРјС‹Р№ СЃРЅРёРјРѕРє РєР»Р°СЃСЃРёС‡РµСЃРєРёС… РѕР±СЉСЏРІР»РµРЅРёР№.
     *
     * @return РѕР±СЉСЏРІР»РµРЅРёСЏ
     */
    public List<ClassicalDeclaration> classicalDeclarations() {
        return List.copyOf(classicalDeclarations);
    }

    /**
     * Р”РѕР±Р°РІР»СЏРµС‚ РїРѕРґРїСЂРѕРіСЂР°РјРјСѓ СѓСЂРѕРІРЅСЏ РїСЂРѕРіСЂР°РјРјС‹.
     *
     * @param definition РїРѕРґРїСЂРѕРіСЂР°РјРјР°
     * @return С‚РµРєСѓС‰Р°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public QuantumProgram addCallableDefinition(final CallableDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Callable definition must not be null.");
        }
        ensureUniqueProgramSymbol(definition.name());
        callableDefinitions.add(definition);
        return this;
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєРѕР»РёС‡РµСЃС‚РІРѕ РїРѕРґРїСЂРѕРіСЂР°РјРј.
     *
     * @return РєРѕР»РёС‡РµСЃС‚РІРѕ РїРѕРґРїСЂРѕРіСЂР°РјРј
     */
    public int callableDefinitionCount() {
        return callableDefinitions.size();
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РїРѕРґРїСЂРѕРіСЂР°РјРјСѓ РїРѕ РёРЅРґРµРєСЃСѓ.
     *
     * @param index РёРЅРґРµРєСЃ РїРѕРґРїСЂРѕРіСЂР°РјРјС‹
     * @return РїРѕРґРїСЂРѕРіСЂР°РјРјР°
     */
    public CallableDefinition callableDefinition(final int index) {
        validateIndex(
            index,
            callableDefinitions.size(),
            "Callable definition"
        );
        return callableDefinitions.get(index);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РЅРµРёР·РјРµРЅСЏРµРјС‹Р№ СЃРЅРёРјРѕРє РїРѕРґРїСЂРѕРіСЂР°РјРј.
     *
     * @return РїРѕРґРїСЂРѕРіСЂР°РјРјС‹
     */
    public List<CallableDefinition> callableDefinitions() {
        return List.copyOf(callableDefinitions);
    }

    /**
     * Р”РѕР±Р°РІР»СЏРµС‚ РІРЅРµС€РЅРµРµ РѕР±СЉСЏРІР»РµРЅРёРµ.
     *
     * @param declaration РІРЅРµС€РЅРµРµ РѕР±СЉСЏРІР»РµРЅРёРµ
     * @return С‚РµРєСѓС‰Р°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public QuantumProgram addExternalCallableDeclaration(final ExternalCallableDeclaration declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("External callable declaration must not be null.");
        }
        ensureUniqueProgramSymbol(declaration.name());
        externalCallableDeclarations.add(declaration);
        return this;
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєРѕР»РёС‡РµСЃС‚РІРѕ РІРЅРµС€РЅРёС… РѕР±СЉСЏРІР»РµРЅРёР№.
     *
     * @return РєРѕР»РёС‡РµСЃС‚РІРѕ РѕР±СЉСЏРІР»РµРЅРёР№
     */
    public int externalCallableDeclarationCount() {
        return externalCallableDeclarations.size();
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РІРЅРµС€РЅРµРµ РѕР±СЉСЏРІР»РµРЅРёРµ РїРѕ РёРЅРґРµРєСЃСѓ.
     *
     * @param index РёРЅРґРµРєСЃ РѕР±СЉСЏРІР»РµРЅРёСЏ
     * @return РІРЅРµС€РЅРµРµ РѕР±СЉСЏРІР»РµРЅРёРµ
     */
    public ExternalCallableDeclaration externalCallableDeclaration(final int index) {
        validateIndex(
            index,
            externalCallableDeclarations.size(),
            "External callable declaration"
        );
        return externalCallableDeclarations.get(index);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РЅРµРёР·РјРµРЅСЏРµРјС‹Р№ СЃРЅРёРјРѕРє РІРЅРµС€РЅРёС… РѕР±СЉСЏРІР»РµРЅРёР№.
     *
     * @return РІРЅРµС€РЅРёРµ РѕР±СЉСЏРІР»РµРЅРёСЏ
     */
    public List<ExternalCallableDeclaration> externalCallableDeclarations() {
        return List.copyOf(externalCallableDeclarations);
    }

    /**
     * Р”РѕР±Р°РІР»СЏРµС‚ РєР°Р»РёР±СЂРѕРІРєСѓ.
     *
     * @param definition РєР°Р»РёР±СЂРѕРІРєР°
     * @return С‚РµРєСѓС‰Р°СЏ РїСЂРѕРіСЂР°РјРјР°
     */
    public QuantumProgram addCalibrationDefinition(final CalibrationDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Calibration definition must not be null.");
        }
        calibrationDefinitions.add(definition);
        return this;
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєРѕР»РёС‡РµСЃС‚РІРѕ РєР°Р»РёР±СЂРѕРІРѕРє.
     *
     * @return РєРѕР»РёС‡РµСЃС‚РІРѕ РєР°Р»РёР±СЂРѕРІРѕРє
     */
    public int calibrationDefinitionCount() {
        return calibrationDefinitions.size();
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РєР°Р»РёР±СЂРѕРІРєСѓ РїРѕ РёРЅРґРµРєСЃСѓ.
     *
     * @param index РёРЅРґРµРєСЃ РєР°Р»РёР±СЂРѕРІРєРё
     * @return РєР°Р»РёР±СЂРѕРІРєР°
     */
    public CalibrationDefinition calibrationDefinition(final int index) {
        validateIndex(
            index,
            calibrationDefinitions.size(),
            "Calibration definition"
        );
        return calibrationDefinitions.get(index);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РЅРµРёР·РјРµРЅСЏРµРјС‹Р№ СЃРЅРёРјРѕРє РєР°Р»РёР±СЂРѕРІРѕРє.
     *
     * @return РєР°Р»РёР±СЂРѕРІРєРё
     */
    public List<CalibrationDefinition> calibrationDefinitions() {
        return List.copyOf(calibrationDefinitions);
    }

    /**
     * Возвращает количество схем в программе.
     *
     * @return количество схем
     */
    public int circuitCount() {
        return circuits.size();
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ СЃС…РµРјСѓ РїРѕ РёРЅРґРµРєСЃСѓ.
     *
     * @param index РёРЅРґРµРєСЃ СЃС…РµРјС‹
     * @return СЃС…РµРјР°
     */
    public QuantumCircuit circuit(final int index) {
        validateCircuitIndex(index);
        return circuits.get(index);
    }

    /**
     * Р’РѕР·РІСЂР°С‰Р°РµС‚ РЅРµРёР·РјРµРЅСЏРµРјС‹Р№ СЃРЅРёРјРѕРє СЃРїРёСЃРєР° СЃС…РµРј.
     *
     * @return СЃРїРёСЃРѕРє СЃС…РµРј
     */
    public List<QuantumCircuit> circuits() {
        return List.copyOf(circuits);
    }

    private void ensureGateBased() {
        if (computationModel != QuantumComputationModel.GATE_BASED_CIRCUIT) {
            throw new IllegalStateException("Only GATE_BASED_CIRCUIT programs can create quantum circuits at this stage.");
        }
    }

    private void validateCircuitIndex(final int index) {
        if (
            index < 0
            || index >= circuits.size()
        ) {
            throw new IllegalArgumentException("Circuit index is outside of program bounds.");
        }
    }

    private void validateGateDefinitionIndex(final int index) {
        validateIndex(
            index,
            gateDefinitions.size(),
            "Gate definition"
        );
    }

    private void validateIndex(
        final int index,
        final int size,
        final String subject
    ) {
        if (
            index < 0
            || index >= size
        ) {
            throw new IllegalArgumentException(subject + " index is outside of program bounds.");
        }
    }

    private void ensureUniqueProgramSymbol(final String name) {
        for (int i = 0; i < gateDefinitions.size(); i++) {
            if (gateDefinitions.get(i).gateName().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
        for (int i = 0; i < classicalDeclarations.size(); i++) {
            if (classicalDeclarations.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
        for (int i = 0; i < callableDefinitions.size(); i++) {
            if (callableDefinitions.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
        for (int i = 0; i < externalCallableDeclarations.size(); i++) {
            if (externalCallableDeclarations.get(i).name().equals(name)) {
                throw new IllegalArgumentException("Program symbol name already exists: " + name + ".");
            }
        }
    }
}