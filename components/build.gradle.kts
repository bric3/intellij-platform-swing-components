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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
}

group = "io.github.bric3.intellij-platform"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.annotations)
}

kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName = "ignored"
    version = properties("platformVersion")
    type = properties("platformType")

    instrumentCode = false
}

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
            )
        }
    }

    val javadocJar by registering(Jar::class) {
        // Note that we publish the Dokka HTML artifacts as Javadoc
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
    }

    assemble {
        dependsOn(javadocJar)
    }

    val jar by getting(Jar::class) {
        archiveBaseName.set("swing-components")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        manifest {
            attributes["Implementation-Version"] = version
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Platform"] = attributes["Build-SDK"] ?: "unknown"
            // clear gradle-intellij-plugin keys
            attributes.keys.removeIf { it.startsWith("Build-") }
            println("Manifest attributes: ${attributes["Implementation-Title"]}")
        }
    }

    listOf(
        runIde,
        prepareSandbox,
        prepareTestingSandbox,
        runPluginVerifier,
        buildSearchableOptions,
        jarSearchableOptions,
        verifyPlugin,
        instrumentCode,
        classpathIndexCleanup,
        buildPlugin,
        patchPluginXml,
    ).forEach {
        it {
            enabled = false
        }
    }
}