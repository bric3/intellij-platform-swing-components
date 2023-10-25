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
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    `java-library`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.changelog)
}

group = "io.github.bric3.intellij-platform"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.annotations)
    api(libs.jsvg)
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

val baseName = "swing-components"
tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
            )
        }
    }

    withType<DokkaTask>().configureEach {
        notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/2231")
    }

    val javadocJar by registering(Jar::class) {
        // Note that we publish the Dokka HTML artifacts as Javadoc
        dependsOn(dokkaHtml)
        archiveBaseName.set(baseName)
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
        from(file("$rootDir/LICENSE")) {
            rename("LICENSE", "LICENSE-$baseName")
        }
    }

    val sourceJar by registering(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveBaseName.set(baseName)
        archiveClassifier.set("sources")
        from(sourceSets.main.map(SourceSet::getAllSource))
        from(file("$rootDir/LICENSE")) {
            rename("LICENSE", "LICENSE-$baseName")
        }
    }

    assemble {
        dependsOn(javadocJar, sourceJar)
    }

    val jar by getting(Jar::class) {
        archiveBaseName.set(baseName)
        duplicatesStrategy = DuplicatesStrategy.FAIL
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

        from(file("$rootDir/LICENSE")) {
            rename("LICENSE", "LICENSE-$baseName")
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

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter.get())
            dependencies {
                implementation.add(libs.assertj)
            }
        }

        withType(JvmTestSuite::class) {
            targets.configureEach {
                testTask.configure {
                    systemProperty("gradle.test.suite.report.location", reports.html.outputLocation.get().asFile)

                    testLogging {
                        showStackTraces = true
                        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

                        events = setOf(
                            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
                            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
                        )
                    }
                }
            }
        }
    }
}
