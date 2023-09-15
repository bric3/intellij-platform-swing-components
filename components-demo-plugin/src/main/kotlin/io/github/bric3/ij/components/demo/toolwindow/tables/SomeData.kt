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
package io.github.bric3.ij.components.demo.toolwindow.tables

typealias NumberMapping = Triple<Long, String, String>
typealias EnglishToJapaneseNumber = NumberMapping
typealias JapaneseNumber = NumberMapping

internal object SomeData {

    /**
     * Numbers from 0 to 16 in Japanese, then dozens then, higher orders of magnitude.
     *
     * Using [On Reading](https://en.wikipedia.org/wiki/On_reading)
     */
    val japaneseNumbers: List<JapaneseNumber> = listOf(
        Triple(0, "rei", "れい"),
        Triple(1, "ichi", "いち"),
        Triple(2, "ni", "に"),
        Triple(3, "san", "さん"),
        Triple(4, "shi", "し"),
        Triple(5, "go", "ご"),
        Triple(6, "roku", "ろく"),
        Triple(7, "shichi", "しち"),
        Triple(8, "hachi", "はち"),
        Triple(9, "kyū", "きゅう"),
        Triple(10, "jū", "じゅ"),
        Triple(11, "juichi", "じゅいち"),
        Triple(12, "junii", "じゅにい"),
        Triple(13, "jusan", "じゅさん"),
        Triple(14, "jushi", "じゅし"),
        Triple(15, "jugo", "じゅご"),
        Triple(16, "juroku", "じゅろく"),

        Triple(20, "ni-jū", "にじゅう"),
        Triple(30, "san-jū", "さんじゅう"),
        Triple(40, "yon-jū", "よんじゅう"),
        Triple(50, "go-jū", "ごじゅう"),
        Triple(60, "roku-jū", "ろくじゅう"),
        Triple(70, "shichi-jū", "しちじゅう"),
        Triple(80, "hachi-jū", "はちじゅう"),
        Triple(90, "kyuu-jū", "きゅうじゅう"),
        Triple(100, "hyaku", "ひゃく"),
        Triple(500, "go-hyaku", "ごひゃく"),
        Triple(1000, "sen", "せん"),
        Triple(5000, "go-sen", "ごせん"),
        Triple(10_000, "sen", "せん"),
        Triple(100_000, "juu-man", "10万"),
        Triple(1_000_000, "hyaku-man", "100万"),
        Triple(100_000_000, "oku", "おく"),
        Triple(1_000_000_000_000, "chō", "ちょう"),
        Triple(10_000_000_000_000_000, "kei", "けい"),
    )

    /**
     * Numbers from 0 to 16 in English To Japanese, then dozens then, higher orders of magnitude.
     */
    val englishToJapanese: List<EnglishToJapaneseNumber> = listOf(
        Triple(0, "zero", "rei"),
        Triple(1, "one", "ichi"),
        Triple(2, "two", "ni"),
        Triple(3, "three", "san"),
        Triple(4, "four", "shi"),
        Triple(5, "five", "go"),
        Triple(6, "six", "roku"),
        Triple(7, "seven", "shichi"),
        Triple(8, "eight", "hachi"),
        Triple(9, "nine", "kyū"),
        Triple(10, "ten", "jū"),
        Triple(11, "eleven", "juichi"),
        Triple(12, "twelve", "junii"),
        Triple(13, "thirteen", "jusan"),
        Triple(14, "fourteen", "jushi"),
        Triple(15, "fifteen", "jugo"),
        Triple(16, "sixteen", "juroku"),

        Triple(20, "twenty", "ni-jū"),
        Triple(30, "thirty", "san-jū"),
        Triple(40, "forty", "yon-jū"),
        Triple(50, "fifty", "go-jū"),
        Triple(60, "sixty", "roku-jū"),
        Triple(70, "seventy", "shichi-jū"),
        Triple(80, "eighty", "hachi-jū"),
        Triple(90, "ninety", "kyuu-jū"),
        Triple(100, "hundred", "hyaku"),
        Triple(500, "five hundred", "go-hyaku"),
        Triple(1000, "thousand", "sen"),
        Triple(5000, "five thousand", "go-sen"),
        Triple(10_000, "ten thousand", "sen"),
        Triple(100_000, "one hundred thousand", "juu-man"),
        Triple(1_000_000, "one million", "hyaku-man"),
        Triple(100_000_000, "one hundred million", "oku"),
        Triple(1_000_000_000_000, "one trillion", "chō"),
        Triple(10_000_000_000_000_000, "ten quadrillion", "kei"),
    )
}