/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2024 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
// gradleProperty do not find sub-project gradle.properties
// https://github.com/gradle/gradle/issues/23572
fun ProviderFactory.localGradleProperty(name: String): Provider<String> = provider {
    if (project.hasProperty(name)) project.property(name)?.toString() else null
}

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.jetbrains.intellijPlatform)
    alias(libs.plugins.jetbrains.idea.ext)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {
    implementation(project(":components"))
    implementation(libs.annotations)
    implementation(libs.classgraph)

    intellijPlatform {
        create(
            type = IntelliJPlatformType.IntellijIdeaCommunity,
            version = providers.localGradleProperty("demoPlatformVersion"),
            useInstaller = false
        )
        plugins(providers.localGradleProperty("demoPlatformPlugins").map { it.split(',') }.getOrElse(emptyList()))
        bundledPlugins(providers.localGradleProperty("demoPlatformBundledPlugins").map { it.split(',') }.getOrElse(emptyList()))

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)

        instrumentationTools()
    }

    testImplementation(libs.bundles.junit.jupiter)
}

kotlin {
    jvmToolchain(17)
}

// Read more:
// * https://github.com/JetBrains/intellij-platform-gradle-plugin/
// * https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    // projectName.set("excalidraw-intellij-plugin")
    pluginConfiguration {
        id = "components-demo"
        name = "IntelliJ Platform Swing Components Local Demo"
        version = "0.1.0"

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null } // removes until-build in plugin.xml
        }
    }

    buildSearchableOptions = false
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

    listOf(
        publishPlugin,
        signPlugin,
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