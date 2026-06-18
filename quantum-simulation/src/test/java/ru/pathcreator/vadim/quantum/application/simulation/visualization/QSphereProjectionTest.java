/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.simulation.visualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.pathcreator.vadim.quantum.application.simulation.result.SimulationResult;
import ru.pathcreator.vadim.quantum.application.simulation.result.StateVectorAmplitude;

final class QSphereProjectionTest {

    @Test
    void projectsBellStateOntoOppositePoles() {
        final List<QSpherePoint> points = new QSphereProjection().project(SimulationResult.of(
            2,
            2,
            0,
            Map.of(),
            List.of(
                new StateVectorAmplitude(
                    "00",
                    Math.sqrt(0.5),
                    0.0
                ),
                new StateVectorAmplitude(
                    "11",
                    Math.sqrt(0.5),
                    0.0
                )
            ),
            List.of()
        ));

        assertEquals(
            2,
            points.size()
        );
        assertEquals(
            1.0,
            points.get(0).z(),
            1.0e-12
        );
        assertEquals(
            -1.0,
            points.get(1).z(),
            1.0e-12
        );
        for (int i = 0; i < points.size(); i++) {
            assertEquals(
                0.5,
                points.get(i).probability(),
                1.0e-12
            );
        }
    }

    @Test
    void projectsEqualWeightStatesOntoSharedLatitudeWithDistinctAzimuth() {
        final List<QSpherePoint> points = new QSphereProjection().project(SimulationResult.of(
            2,
            0,
            0,
            Map.of(),
            List.of(
                new StateVectorAmplitude(
                    "01",
                    0.0,
                    1.0
                ),
                new StateVectorAmplitude(
                    "10",
                    1.0,
                    0.0
                )
            ),
            List.of()
        ));

        assertEquals(
            2,
            points.size()
        );
        assertEquals(
            0.0,
            points.get(0).z(),
            1.0e-12
        );
        assertEquals(
            0.0,
            points.get(1).z(),
            1.0e-12
        );
        assertEquals(
            Math.PI / 2.0,
            points.get(0).phase(),
            1.0e-12
        );
        assertTrue(Math.abs(points.get(0).x() - points.get(1).x()) > 1.0e-9);
    }

    @Test
    void projectsCompleteThreeQubitStateWithNormalizedProbabilitiesAndFiniteCoordinates() {
        final double amplitude = 1.0 / Math.sqrt(8.0);
        final List<QSpherePoint> points = new QSphereProjection().project(SimulationResult.of(
            3,
            0,
            0,
            Map.of(),
            List.of(
                new StateVectorAmplitude("000", amplitude, 0.0),
                new StateVectorAmplitude("001", amplitude, 0.0),
                new StateVectorAmplitude("010", amplitude, 0.0),
                new StateVectorAmplitude("011", amplitude, 0.0),
                new StateVectorAmplitude("100", amplitude, 0.0),
                new StateVectorAmplitude("101", amplitude, 0.0),
                new StateVectorAmplitude("110", amplitude, 0.0),
                new StateVectorAmplitude("111", amplitude, 0.0)
            ),
            List.of()
        ));

        double probabilitySum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            probabilitySum += points.get(i).probability();
            assertTrue(Double.isFinite(points.get(i).x()));
            assertTrue(Double.isFinite(points.get(i).y()));
            assertTrue(Double.isFinite(points.get(i).z()));
            assertTrue(Double.isFinite(points.get(i).phase()));
        }
        assertEquals(
            1.0,
            probabilitySum,
            1.0e-12
        );
        assertEquals(
            1.0,
            findPoint(
                points,
                "000"
            ).z(),
            1.0e-12
        );
        assertEquals(
            -1.0,
            findPoint(
                points,
                "111"
            ).z(),
            1.0e-12
        );
        assertEquals(
            1.0 / 3.0,
            findPoint(
                points,
                "001"
            ).z(),
            1.0e-12
        );
        assertEquals(
            -1.0 / 3.0,
            findPoint(
                points,
                "011"
            ).z(),
            1.0e-12
        );
    }

    private static QSpherePoint findPoint(
        final List<QSpherePoint> points,
        final String basisState
    ) {
        for (int i = 0; i < points.size(); i++) {
            if (basisState.equals(points.get(i).basisState())) {
                return points.get(i);
            }
        }
        throw new IllegalArgumentException("Q-sphere point not found: " + basisState + ".");
    }
}