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
package io.github.bric3.ij.components.utils

import kotlin.math.ln
import kotlin.math.pow

/**
 * Simple utility class to convert linear values to logarithmic values and vice versa.
 *
 * The class handles special values like `0` and `1`.
 */
class LogScale {
    /**
     * Adjusting constant to avoid `log(0) = -Infinity` (undefined) and `log(1) = 0`.
     *
     * The amount of this constant is somewhat arbitrary, it was chosen to be small enough,
     * so that it doesn't affect the logarithmic scale too much, but big enough to prevent
     * undefined or undesired results.
     *
     * This process can be seen as a form of smoothing because it could smooth `0` into
     * a more gradual change.
     */
    private val constant = 1e-10

    constructor(min: Long, max: Long) {
        this.min = min
        this.max = max
        checkMinMax(min, max)
    }

    var min: Long = 0
        set(value) {
            checkMinMax(value, max)
            field = value
        }

    var max: Long = 0
        set(value) {
            checkMinMax(min, value)
            field = value
        }

    private fun checkMinMax(min: Long, max: Long) {
        require(min <= max) { "min ($min) must be less than or equal to max ($max)" }
    }

    fun range(): Long {
        return max - min
    }

    /**
     * Maps logarithmic 0 - 1 point to a linear number between min and max.
     *
     * * `f(0) = minValue`
     * * `f(1) = maxValue`
     * * `f(x) = (range + 1)^x + minValue`
     *
     * @param logValue from `0` to `1` in log scale
     * @returns between min and max inclusive
     */
    fun logarithmicToLinear(logValue: Double): Long {
        var value = Math.round((range() + constant).pow(logValue) + min - constant)

        if (value < min) {
            value = min
        } else if (value > max) {
            value = max
        }

        return value.coerceIn(min, max)
    }

    /**
     * Maps a linear value to a logarithmic point between 0-1
     *
     * * `g(minValue) = 0`
     * * `g(maxValue) = 1`
     * * `g(x) = log(base range + 1)(x)`
     *
     * @param value linear value between min and max
     * @returns Logarithmic value between 0 to 1
     */
    fun linearToLogarithmic(linearValue: Long): Double {
        val normalizedValue = linearValue - min + constant

        return if (normalizedValue <= 0) {
            0.0
        } else if (linearValue >= max) {
            1.0
        } else {
            ln(normalizedValue) / ln((range() + constant))
        }
    }
}