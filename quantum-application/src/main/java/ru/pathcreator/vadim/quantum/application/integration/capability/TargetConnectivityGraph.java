/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable граф физической связности qubit target-а.
 */
public final class TargetConnectivityGraph {

    private static final String EDGE_SEPARATOR = "->";

    private final boolean constrained;
    private final boolean directed;
    private final Set<String> edges;

    private TargetConnectivityGraph(
        final boolean constrained,
        final boolean directed,
        final Set<String> edges
    ) {
        this.constrained = constrained;
        this.directed = directed;
        this.edges = Set.copyOf(edges);
    }

    /**
     * Создает граф без ограничений связности.
     *
     * @return all-to-all граф
     */
    public static TargetConnectivityGraph allToAll() {
        return new TargetConnectivityGraph(
            false,
            false,
            Set.of()
        );
    }

    /**
     * Создает неориентированный граф из пар qubit индексов.
     *
     * @param edges пары индексов вида {left, right}
     * @return граф связности
     */
    public static TargetConnectivityGraph undirected(final long[][] edges) {
        return of(
            false,
            edges
        );
    }

    /**
     * Создает ориентированный граф из пар qubit индексов.
     *
     * @param edges пары индексов вида {from, to}
     * @return граф связности
     */
    public static TargetConnectivityGraph directed(final long[][] edges) {
        return of(
            true,
            edges
        );
    }

    private static TargetConnectivityGraph of(
        final boolean directed,
        final long[][] edges
    ) {
        if (edges == null) {
            throw new IllegalArgumentException("Target connectivity edges must not be null.");
        }
        final HashSet<String> copy = new HashSet<>();
        for (int i = 0; i < edges.length; i++) {
            if (
                edges[i] == null
                || edges[i].length != 2
            ) {
                throw new IllegalArgumentException("Target connectivity edge must contain exactly two indices.");
            }
            copy.add(edgeKey(
                edges[i][0],
                edges[i][1],
                directed
            ));
        }
        return new TargetConnectivityGraph(
            true,
            directed,
            copy
        );
    }

    /**
     * Проверяет, есть ли ограничения связности.
     *
     * @return true, если target имеет явный connectivity graph
     */
    public boolean isConstrained() {
        return constrained;
    }

    /**
     * Проверяет, является ли граф ориентированным.
     *
     * @return true для directed graph
     */
    public boolean isDirected() {
        return directed;
    }

    /**
     * Проверяет, разрешено ли взаимодействие двух физических индексов.
     *
     * @param first первый индекс
     * @param second второй индекс
     * @return true, если взаимодействие разрешено
     */
    public boolean supportsInteraction(
        final long first,
        final long second
    ) {
        validateIndex(first);
        validateIndex(second);
        if (!constrained) {
            return true;
        }
        return edges.contains(edgeKey(
            first,
            second,
            directed
        ));
    }

    /**
     * Возвращает количество ребер графа.
     *
     * @return количество ребер
     */
    public int edgeCount() {
        return edges.size();
    }

    private static String edgeKey(
        final long first,
        final long second,
        final boolean directed
    ) {
        validateIndex(first);
        validateIndex(second);
        if (directed || first <= second) {
            return first + EDGE_SEPARATOR + second;
        }
        return second + EDGE_SEPARATOR + first;
    }

    private static void validateIndex(final long index) {
        if (index < 0) {
            throw new IllegalArgumentException("Target connectivity qubit index must not be negative.");
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TargetConnectivityGraph graph)) {
            return false;
        }
        return constrained == graph.constrained
            && directed == graph.directed
            && Objects.equals(
                edges,
                graph.edges
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            constrained,
            directed,
            edges
        );
    }
}