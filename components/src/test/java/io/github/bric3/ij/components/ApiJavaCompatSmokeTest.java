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

import org.junit.Test;

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
}
