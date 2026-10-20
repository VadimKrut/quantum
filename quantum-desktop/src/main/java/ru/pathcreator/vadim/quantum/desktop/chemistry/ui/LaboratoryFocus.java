/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.chemistry.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

/** Local focus follows workspace pages; no native focus or desktop input is requested. */
final class LaboratoryFocus {
  private final BorderPane root;
  private final Node workArea, fallback;
  private Node returnTo, marked, pending;
  private long generation;
  private final javafx.beans.value.ChangeListener<Node> focusChanged =
      (observable, before, after) -> this.mark(after);

  LaboratoryFocus(final BorderPane root, final Node workArea, final Node fallback) {
    this.root = root;
    this.workArea = workArea;
    this.fallback = fallback;
    root.centerProperty()
        .addListener(
            (observable, before, after) -> {
              this.request(after, after == workArea ? this.returnTo : null);
            });
    root.sceneProperty()
        .addListener(
            (observable, before, after) -> {
              if (before != null) {
                before.focusOwnerProperty().removeListener(this.focusChanged);
              }
              this.mark(null);
              if (after != null) {
                after.focusOwnerProperty().addListener(this.focusChanged);
                this.request(root.getCenter(), null);
              }
            });
    if (root.getScene() != null) {
      root.getScene().focusOwnerProperty().addListener(this.focusChanged);
    }
  }

  void request(final Node page, final Node preferred) {
    final long requested = ++this.generation;
    this.pending = preferred;
    Platform.runLater(
        () -> {
          if (requested != this.generation
              || this.root.getScene() == null
              || this.root.getCenter() != page
              || page == null) {
            return;
          }
          this.root.applyCss();
          final Node target;
          if (contains(this.root, preferred) && available(preferred)) {
            target = preferred;
          } else if (page == this.workArea && available(this.fallback)) {
            target = this.fallback;
          } else {
            target = first(page);
          }
          this.pending = null;
          if (target != null) {
            target.requestFocus();
          }
        });
  }

  Node capture() {
    if (contains(this.root, this.pending) && available(this.pending)) {
      return this.pending;
    }
    final Node current = this.root.getScene() == null ? null : this.root.getScene().getFocusOwner();
    if (contains(this.root, current) && available(current)) {
      return current;
    }
    return contains(this.root, this.returnTo) && available(this.returnTo) ? this.returnTo : null;
  }

  private void mark(final Node node) {
    if (this.pending != null
        && node != this.pending
        && contains(this.root, node)
        && available(node)) {
      this.pending = null;
      this.generation++;
    }
    if (this.marked != null) {
      this.marked.getStyleClass().remove("laboratory-keyboard-focus");
    }
    this.marked = contains(this.root, node) ? node : null;
    if (this.root.getCenter() == this.workArea && this.marked != null && available(this.marked)) {
      this.returnTo = this.marked;
    }
    if (this.marked != null && !this.marked.getStyleClass().contains("laboratory-keyboard-focus")) {
      this.marked.getStyleClass().add("laboratory-keyboard-focus");
    }
  }

  private static Node first(final Node page) {
    for (final String id :
        new String[] {
          "chemistry-file-confirm-cancel",
          "chemistry-recovery-choices",
          "chemistry-file-operation-cancel",
          "chemistry-text-input"
        }) {
      final Node candidate = page.lookup("#" + id);
      if (available(candidate)) {
        return candidate;
      }
    }
    return descendant(page);
  }

  private static Node descendant(final Node node) {
    if (!available(node)) {
      return null;
    }
    if (node instanceof Control && !(node instanceof ScrollPane) && node.isFocusTraversable()) {
      return node;
    }
    if (node instanceof Parent parent) {
      for (final Node child : parent.getChildrenUnmodifiable()) {
        final Node candidate = descendant(child);
        if (candidate != null) {
          return candidate;
        }
      }
    }
    return node.isFocusTraversable() ? node : null;
  }

  static boolean contains(final Node parent, final Object target) {
    if (!(target instanceof Node node)) {
      return false;
    }
    while (node != null) {
      if (node == parent) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }

  private static boolean available(final Node node) {
    if (node == null) {
      return false;
    }
    for (Node current = node; current != null; current = current.getParent()) {
      if (!current.isVisible() || current.isDisabled()) {
        return false;
      }
    }
    return true;
  }

  static boolean localInput(final Object target) {
    if (!(target instanceof Node node)) {
      return false;
    }
    while (node != null) {
      if (node instanceof TextInputControl
          || node instanceof ComboBoxBase<?>
          || node instanceof ListView<?>
          || node instanceof TableView<?>
          || node instanceof TreeView<?>
          || node instanceof TreeTableView<?>
          || node instanceof Spinner<?>
          || node instanceof Slider) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }
}
