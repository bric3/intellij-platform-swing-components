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
import io.gitlab.arturbosch.detekt.Detekt

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

val detektPlugins: Configuration = configurations.getByName("detektPlugins")

dependencies {
    detektPlugins(libs.bundles.detektplugins)
}

repositories {
    mavenCentral()
}

spotless {
    java {
        licenseHeaderFile(project.file("$rootDir/.spotless/java.HEADER"))
        target("**/*.java")
    }
    kotlin {
        licenseHeaderFile(project.file("$rootDir/.spotless/java.HEADER"))
        target("**/*.kt", "**/*.kts")
        targetExclude(
            "**/*.gradle.kts",
            "**/SvgIcon.kt",
        )
    }
    kotlinGradle {
        licenseHeaderFile(project.file("$rootDir/.spotless/java.HEADER"),
            "(@file|fun|package|import|plugins|pluginManagement) "
        )
        target("**/*.gradle.kts")
    }
    // format("license") {
    //     target("**/*.java", "**/*.kt", "**/*.kts")
    // }
    // format("markdown") {
    //     licenseHeaderFile(project.file("HEADER"), ".").apply {
    //         updateYearWithLatest(true)
    //     }
    //     target("**/*.md")
    // }
}

tasks {
    val detektAll by registering(Detekt::class) {
        source(files(project.projectDir))

        include("**/*.kt")
        include("**/*.kts")
        exclude("**/build/**")
        exclude("**/generated/**")
        exclude("**/testData/**")

        config.setFrom("$rootDir/.detekt/detekt.yml")
        buildUponDefaultConfig = false
        autoCorrect = true
    }
}