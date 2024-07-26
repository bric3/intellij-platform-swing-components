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
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
// gradleProperty do not find sub-project gradle.properties
// https://github.com/gradle/gradle/issues/23572
fun ProviderFactory.localGradleProperty(name: String): Provider<String> = provider {
    if (project.hasProperty(name)) project.property(name)?.toString() else null
}

plugins {
    `java-library`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.jetbrains.changelog)
    alias(libs.plugins.jetbrains.intellijPlatform)
    alias(libs.plugins.jetbrains.idea.ext)
}


group = "io.github.bric3.intellij-platform"

repositories {
    mavenCentral()
    println(this::class.java.classLoader)
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    compileOnly(libs.annotations)
    api(libs.jsvg)

    intellijPlatform {
        create(
            type = IntelliJPlatformType.IntellijIdeaCommunity,
            version = providers.localGradleProperty("platformVersion"),
            useInstaller = false
        )

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)

        pluginVerifier()
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
    instrumentCode = false
    buildSearchableOptions = false
}

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

val baseName = "swing-components"
tasks {
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
            )
            javaParameters = true
        }
    }

    withType<DokkaTask>().configureEach {
        notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/2231")
    }

    withType<DokkaTaskPartial>().configureEach {
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

    // this is a library for a plugin, not a plugin
    listOf(
        runIde,
        prepareSandbox,
        // don't disable prepareTestSandbox it is needed for tests
        prepareTestIdeUiSandbox,
        prepareTestIdePerformanceSandbox,
        buildSearchableOptions,
        jarSearchableOptions,
        publishPlugin,
        signPlugin,
        verifyPlugin,
        instrumentCode,
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
