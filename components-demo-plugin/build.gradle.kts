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
fun properties(key: String) = providers.gradleProperty(key)

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotations)
    implementation(project(":components"))
    implementation(libs.classgraph)
}

kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName = "IntelliJ Platform Swing Components Demo"
    version = properties("demoPlatformVersion")
    type = "IC"
    updateSinceUntilBuild = true
}

tasks {
    test {
        useJUnitPlatform()
    }

    runIde {
        jvmArgs(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+IgnoreUnrecognizedVMOptions",
            "-XX:+AllowEnhancedClassRedefinition" // for DCEVM
        )
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")
    }

    listOf(
        runPluginVerifier,
        verifyPlugin,
        instrumentCode,
        buildSearchableOptions,
        assemble,
    ).forEach {
        it {
            enabled = false
        }
    }
}