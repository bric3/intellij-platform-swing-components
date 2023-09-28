/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2023 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package io.github.bric3.ij.components;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

public class ApiJavaCompatSmokeTest {
    @Test
    public void for_ColoredJProgressBar() {
        ColoredJProgressBar progressBar = new ColoredJProgressBar(1, 100);
        progressBar.adjustPreferredWidth(100);
        progressBar.adjustMaximumWidth(100);
        progressBar.setFinishedColor(Color.black);
        progressBar.getFinishedColor();
        progressBar.setRemainderColor(Color.red);
        progressBar.getRemainderColor();
        progressBar.setRatio(0.4);
        progressBar.getRatio();
    }

    @Test
    public void for_HoveringToolbar() {
        var table = new JTable();
        var scroller = new JScrollPane(table);
        HoveringToolbar hoveringToolbar = HoveringToolbar.wrap(
                scroller,
                table,
                ActionManager.getInstance().createActionToolbar("my loc", new DefaultActionGroup(), true)
        );

        hoveringToolbar.getTable();
        hoveringToolbar.getToolbar();
        hoveringToolbar.getContainer();

        hoveringToolbar.setToolbarBackground(Color.red);
        hoveringToolbar.getToolbarBackground();
        hoveringToolbar.setToolbarOpaque(true);
        hoveringToolbar.isToolbarOpaque();
    }

    @Test
    public void for_VerticalExpandButton() {
        var button = new VerticalExpandButton("Whatever", SwingConstants.LEFT, false);
        button.bindComponentVisibility(new JPanel());
        button.createCollapseAction(() -> "Hide Panel", () -> { /* when collapsed */ });
        button.getExpanded().set(true);
    }
}
